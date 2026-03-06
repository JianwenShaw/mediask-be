package me.jianwen.mediask.infra.schedule.engine.solver;

import me.jianwen.mediask.schedule.domain.optimization.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.ConstraintExpression;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.ConstraintRule;
import me.jianwen.mediask.schedule.domain.optimization.engine.solver.SolverPlugin;
import me.jianwen.mediask.schedule.domain.optimization.engine.solver.SolverPluginResult;
import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationResult;
import me.jianwen.mediask.schedule.domain.rule.DoctorAvailabilityRule;
import me.jianwen.mediask.schedule.domain.rule.DoctorTimeOff;
import me.jianwen.mediask.schedule.domain.optimization.OptimizationAssignment;
import me.jianwen.mediask.schedule.domain.optimization.OptimizationUnfilledSlot;
import me.jianwen.mediask.schedule.domain.optimization.ScheduleDoctorProfile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 启发式求解器抽象基类。
 */
public abstract class AbstractHeuristicSolverPlugin implements SolverPlugin {

    private static final WeekFields ISO_WEEK = WeekFields.ISO;
    private static final int PARALLEL_THRESHOLD = 16;

    private final Executor scheduleSolverExecutor;

    protected AbstractHeuristicSolverPlugin(Executor scheduleSolverExecutor) {
        this.scheduleSolverExecutor = scheduleSolverExecutor;
    }

    protected SolverPluginResult solveWithMultiStart(
            SchedulingEngineRequest request,
            CompiledConstraintModel model,
            int attempts) {
        int safeAttempts = Math.max(1, attempts);
        long baseSeed = request.solverConfig().seed() != null
                ? request.solverConfig().seed()
                : deriveSeed(request, model.sourceHash());

        SolveAttempt best = null;
        long deadlineMs = System.currentTimeMillis() + Math.max(request.solverConfig().timeLimitMs(), 50L);
        for (int i = 0; i < safeAttempts; i++) {
            if (System.currentTimeMillis() > deadlineMs) {
                break;
            }
            SolveAttempt current = solveOnce(request, model, baseSeed, i);
            if (best == null || compareAttempt(current, best) > 0) {
                best = current;
            }
        }

        if (best == null) {
            best = solveOnce(request, model, baseSeed, 0);
        }

        List<String> warnings = new ArrayList<>(best.warnings());
        if (!best.minimalConflictSet().isEmpty()) {
            warnings.add("最小冲突集(近似): " + String.join(",", best.minimalConflictSet()));
        }
        DepartmentScheduleOptimizationResult result = new DepartmentScheduleOptimizationResult(
                best.assignments(),
                best.unfilledSlots(),
                best.scoreBreakdown(),
                best.totalScore(),
                best.hardViolationCount(),
                warnings
        );
        return new SolverPluginResult(result, warnings, best.minimalConflictSet());
    }

    private int compareAttempt(SolveAttempt left, SolveAttempt right) {
        if (left.hardViolationCount() != right.hardViolationCount()) {
            return Integer.compare(right.hardViolationCount(), left.hardViolationCount());
        }
        return Double.compare(left.totalScore(), right.totalScore());
    }

    private SolveAttempt solveOnce(
            SchedulingEngineRequest request,
            CompiledConstraintModel model,
            long baseSeed,
            int attemptIndex) {
        SolverState state = new SolverState();
        RuleResource resource = buildResource(request, model);
        List<OptimizationAssignment> assignments = new ArrayList<>();
        List<OptimizationUnfilledSlot> unfilledSlots = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<SlotKey, DemandSpec> effectiveDemandMap = new HashMap<>();
        Map<String, Integer> hardConflictCounter = new HashMap<>();

        LocalDate cursor = request.optimizationRequest().startDate();
        List<Integer> sortedPeriods = request.optimizationRequest().periods().stream().sorted().toList();
        while (!cursor.isAfter(request.optimizationRequest().endDate())) {
            HolidayPolicySettings holidayPolicy = resolveHolidayPolicy(resource, request);
            boolean effectiveHoliday = isEffectiveHoliday(cursor, request);

            for (Integer periodCode : sortedPeriods) {
                SlotKey slotKey = new SlotKey(cursor, periodCode);
                DemandSpec demand = adjustDemandForHoliday(
                        resource.demandMap().getOrDefault(slotKey, DemandSpec.defaultSpec()),
                        effectiveHoliday,
                        holidayPolicy
                );
                effectiveDemandMap.put(slotKey, demand);

                if (demand.requiredDoctors() <= 0) {
                    continue;
                }

                List<Long> pickedDoctors = new ArrayList<>();
                int neededSenior = Math.max(0, demand.minSeniorDoctors());

                for (int i = 0; i < demand.requiredDoctors(); i++) {
                    CandidateSelection selection = selectBestCandidate(
                            request,
                            model,
                            resource,
                            state,
                            pickedDoctors,
                            cursor,
                            periodCode,
                            neededSenior,
                            baseSeed,
                            attemptIndex
                    );

                    if (selection.candidate() == null) {
                        int missing = demand.requiredDoctors() - i;
                        mergeCounter(hardConflictCounter, selection.rejectionCounter());
                        String reason = buildUnfilledReason(selection.rejectionCounter());
                        unfilledSlots.add(new OptimizationUnfilledSlot(cursor, periodCode, missing, reason));
                        break;
                    }

                    CandidateScore chosen = selection.candidate();
                    pickedDoctors.add(chosen.doctor().doctorId());
                    if (chosen.doctor().senior() && neededSenior > 0) {
                        neededSenior--;
                    }

                    assignments.add(new OptimizationAssignment(
                            cursor,
                            periodCode,
                            chosen.doctor().doctorId(),
                            chosen.reasons(),
                            chosen.penalties()
                    ));
                    state.markAssigned(chosen.doctor().doctorId(), cursor, periodCode);
                }
            }
            cursor = cursor.plusDays(1);
        }

        int hardViolationCount = unfilledSlots.stream().mapToInt(OptimizationUnfilledSlot::missingDoctors).sum();
        if (hardViolationCount > 0) {
            warnings.add("存在未满足排班需求，请根据冲突报告进行人工调整");
        }

        List<String> minimalConflictSet = hardConflictCounter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        Map<String, Double> scoreBreakdown = evaluateScores(
                assignments,
                unfilledSlots,
                request.doctors(),
                effectiveDemandMap,
                state.totalShiftCounter(),
                state.weekendShiftCounter(),
                resource.preferenceMap()
        );
        double totalScore = weightedTotal(scoreBreakdown, model.objective().weights());

        return new SolveAttempt(
                assignments,
                unfilledSlots,
                scoreBreakdown,
                totalScore,
                hardViolationCount,
                warnings,
                minimalConflictSet
        );
    }

    private CandidateSelection selectBestCandidate(
            SchedulingEngineRequest request,
            CompiledConstraintModel model,
            RuleResource resource,
            SolverState state,
            List<Long> pickedDoctors,
            LocalDate date,
            Integer periodCode,
            int neededSenior,
            long baseSeed,
            int attemptIndex) {
        List<CandidateScore> candidates;
        if (request.doctors().size() >= PARALLEL_THRESHOLD) {
            List<CompletableFuture<CandidateScore>> futures = new ArrayList<>();
            for (ScheduleDoctorProfile doctor : request.doctors()) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> evaluateCandidate(
                                request,
                                model,
                                resource,
                                state,
                                pickedDoctors,
                                date,
                                periodCode,
                                neededSenior,
                                doctor,
                                baseSeed,
                                attemptIndex
                        ),
                        scheduleSolverExecutor
                ));
            }
            candidates = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        } else {
            List<CandidateScore> tmp = new ArrayList<>();
            for (ScheduleDoctorProfile doctor : request.doctors()) {
                CandidateScore score = evaluateCandidate(
                        request,
                        model,
                        resource,
                        state,
                        pickedDoctors,
                        date,
                        periodCode,
                        neededSenior,
                        doctor,
                        baseSeed,
                        attemptIndex
                );
                if (score != null) {
                    tmp.add(score);
                }
            }
            candidates = tmp;
        }

        Map<String, Integer> rejectionCounter = new HashMap<>();
        if (candidates.isEmpty()) {
            for (ScheduleDoctorProfile doctor : request.doctors()) {
                if (pickedDoctors.contains(doctor.doctorId())) {
                    continue;
                }
                CandidateReject reject = evaluateReject(
                        model,
                        resource,
                        state,
                        date,
                        periodCode,
                        doctor
                );
                for (String ruleId : reject.ruleIds()) {
                    rejectionCounter.put(ruleId, rejectionCounter.getOrDefault(ruleId, 0) + 1);
                }
            }
            return new CandidateSelection(null, rejectionCounter);
        }

        CandidateScore best = candidates.stream()
                .max(Comparator
                        .comparingDouble(CandidateScore::score)
                        .thenComparing(candidate -> candidate.doctor().doctorId(), Comparator.reverseOrder()))
                .orElse(null);
        return new CandidateSelection(best, rejectionCounter);
    }

    private CandidateScore evaluateCandidate(
            SchedulingEngineRequest request,
            CompiledConstraintModel model,
            RuleResource resource,
            SolverState state,
            List<Long> pickedDoctors,
            LocalDate date,
            Integer periodCode,
            int neededSenior,
            ScheduleDoctorProfile doctor,
            long baseSeed,
            int attemptIndex) {
        if (pickedDoctors.contains(doctor.doctorId())) {
            return null;
        }
        if (state.containsAssignment(date, periodCode, doctor.doctorId())) {
            return null;
        }

        Map<String, RuleCheckResult> hardResultMap = new LinkedHashMap<>();
        for (ConstraintRule rule : sortedRules(model.hardRules())) {
            hardResultMap.put(rule.id(), evaluateHardRule(rule, resource, state, doctor, date, periodCode));
        }

        if (!evaluateExpression(model.hardExpression(), hardResultMap)) {
            return null;
        }

        List<String> reasons = new ArrayList<>();
        List<String> penalties = new ArrayList<>();
        Map<String, RuleCheckResult> softResultMap = new LinkedHashMap<>();
        double score = 0D;
        for (ConstraintRule rule : sortedRules(model.softRules())) {
            RuleCheckResult result = evaluateSoftRule(rule, resource, state, doctor, date, periodCode, neededSenior);
            softResultMap.put(rule.id(), result);
            score += result.penalty() * rule.weight();
            if (result.satisfied()) {
                reasons.add(result.message());
            } else {
                penalties.add(result.message());
            }
        }

        if (!evaluateExpression(model.softExpression(), softResultMap)) {
            score -= 2D;
            penalties.add("软约束组合表达式未满足");
        }

        score += tieBreaker(baseSeed, attemptIndex, doctor.doctorId(), date, periodCode);
        return new CandidateScore(doctor, score, reasons, penalties);
    }

    private CandidateReject evaluateReject(
            CompiledConstraintModel model,
            RuleResource resource,
            SolverState state,
            LocalDate date,
            Integer periodCode,
            ScheduleDoctorProfile doctor) {
        List<String> failedRules = new ArrayList<>();
        Map<String, RuleCheckResult> hardResultMap = new LinkedHashMap<>();
        for (ConstraintRule rule : sortedRules(model.hardRules())) {
            RuleCheckResult result = evaluateHardRule(rule, resource, state, doctor, date, periodCode);
            hardResultMap.put(rule.id(), result);
            if (!result.satisfied()) {
                failedRules.add(rule.id());
            }
        }
        if (!evaluateExpression(model.hardExpression(), hardResultMap) && failedRules.isEmpty()) {
            failedRules.add("HARD_EXPR");
        }
        return new CandidateReject(failedRules);
    }

    private List<ConstraintRule> sortedRules(Map<String, ConstraintRule> rules) {
        return rules.values().stream()
                .sorted(Comparator.comparingInt(ConstraintRule::priority).reversed().thenComparing(ConstraintRule::id))
                .toList();
    }

    private RuleCheckResult evaluateHardRule(
            ConstraintRule rule,
            RuleResource resource,
            SolverState state,
            ScheduleDoctorProfile doctor,
            LocalDate date,
            Integer periodCode) {
        String type = rule.type().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "DOCTOR_TIME_OFF" -> {
                boolean timeOff = isTimeOff(resource.timeOffMap(), doctor.doctorId(), date, periodCode);
                yield timeOff
                        ? RuleCheckResult.fail("医生请假/停诊冲突", "timeOff")
                        : RuleCheckResult.ok("通过请假校验");
            }
            case "DOCTOR_AVAILABILITY" -> {
                boolean available = isDoctorAvailable(resource.availabilityMap(), doctor.doctorId(), date, periodCode);
                yield available
                        ? RuleCheckResult.ok("通过可排班规则")
                        : RuleCheckResult.fail("医生可排班规则不满足", "availability");
            }
            case "MAX_CONSECUTIVE_DAYS" -> {
                int maxDays = intParam(rule.params(), "maxDays", 5);
                boolean violated = violatesConsecutiveDays(doctor.doctorId(), date, state.assignedDates(), maxDays);
                yield violated
                        ? RuleCheckResult.fail("连续工作天数超限", "maxConsecutiveDays")
                        : RuleCheckResult.ok("连续工作天数满足");
            }
            case "MAX_SHIFTS_PER_WEEK" -> {
                int maxShifts = intParam(rule.params(), "maxShifts", 10);
                DoctorWeekKey weekKey = DoctorWeekKey.of(doctor.doctorId(), date);
                int current = state.weekShiftCounter().getOrDefault(weekKey, 0);
                yield current >= maxShifts
                        ? RuleCheckResult.fail("周班次超限", "maxShiftsPerWeek")
                        : RuleCheckResult.ok("周班次满足");
            }
            case "MIN_REST_HOURS" -> {
                int minHours = intParam(rule.params(), "hours", 12);
                AssignmentPoint latest = state.latestAssignment().get(doctor.doctorId());
                if (latest == null) {
                    yield RuleCheckResult.ok("休息时长满足");
                }
                long diff = hoursBetween(latest.date(), latest.periodCode(), date, periodCode);
                yield diff < minHours
                        ? RuleCheckResult.fail("休息时长不足", "minRestHours")
                        : RuleCheckResult.ok("休息时长满足");
            }
            case "HOLIDAY_POLICY" -> {
                HolidayPolicySettings policy = resolveHolidayPolicy(resource, null);
                if (policy.policy() == HolidayPolicy.CLOSE && resource.holidayDates().contains(date) && !resource.makeupWorkdayDates().contains(date)) {
                    yield RuleCheckResult.fail("节假日策略为停排", "holidayPolicy");
                }
                yield RuleCheckResult.ok("节假日策略满足");
            }
            default -> RuleCheckResult.ok("未识别硬规则，默认放行: " + rule.type());
        };
    }

    private RuleCheckResult evaluateSoftRule(
            ConstraintRule rule,
            RuleResource resource,
            SolverState state,
            ScheduleDoctorProfile doctor,
            LocalDate date,
            Integer periodCode,
            int neededSenior) {
        String type = rule.type().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "FAIRNESS" -> {
                int workload = state.totalShiftCounter().getOrDefault(doctor.doctorId(), 0);
                double score = Math.max(0D, 5D - workload * 0.5D);
                yield RuleCheckResult.ok(score, "公平性得分: 当前班次=" + workload);
            }
            case "PREFERENCE" -> {
                int priority = resource.preferenceMap()
                        .getOrDefault(doctor.doctorId(), Map.of())
                        .getOrDefault(periodCode, 0);
                if (priority > 0) {
                    yield RuleCheckResult.ok(2D + Math.min(priority, 3), "偏好匹配: 优先级=" + priority);
                }
                yield RuleCheckResult.fail(-1.5D, "偏好未命中");
            }
            case "CONTINUITY" -> {
                Set<LocalDate> dates = state.assignedDates().getOrDefault(doctor.doctorId(), Set.of());
                if (dates.contains(date.minusDays(1))) {
                    yield RuleCheckResult.ok(1.5D, "连续性加分: 与前一日衔接");
                }
                yield RuleCheckResult.ok(0.3D, "连续性基础分");
            }
            case "SENIOR_COVERAGE" -> {
                if (neededSenior <= 0) {
                    yield RuleCheckResult.ok(0.2D, "资深覆盖已满足");
                }
                if (doctor.senior()) {
                    yield RuleCheckResult.ok(3.5D, "资深覆盖加分");
                }
                yield RuleCheckResult.fail(-2.0D, "资深覆盖不足");
            }
            case "WEEKEND_BALANCE" -> {
                if (!isWeekend(date)) {
                    yield RuleCheckResult.ok(0D, "非周末时段");
                }
                int weekendShifts = state.weekendShiftCounter().getOrDefault(doctor.doctorId(), 0);
                if (weekendShifts <= 1) {
                    yield RuleCheckResult.ok(1.5D, "周末均衡加分");
                }
                yield RuleCheckResult.fail(-1.0D, "周末班次偏多");
            }
            default -> RuleCheckResult.ok(0D, "未识别软规则: " + rule.type());
        };
    }

    private boolean evaluateExpression(ConstraintExpression expression, Map<String, RuleCheckResult> ruleResultMap) {
        if (expression == null) {
            return true;
        }
        return switch (expression) {
            case ConstraintExpression.RuleRef ruleRef -> {
                RuleCheckResult result = ruleResultMap.get(ruleRef.ruleId());
                yield result != null && result.satisfied();
            }
            case ConstraintExpression.And andExpr -> {
                boolean allMatched = true;
                for (ConstraintExpression child : andExpr.children()) {
                    if (!evaluateExpression(child, ruleResultMap)) {
                        allMatched = false;
                        break;
                    }
                }
                yield allMatched;
            }
            case ConstraintExpression.Or orExpr -> {
                boolean anyMatched = false;
                for (ConstraintExpression child : orExpr.children()) {
                    if (evaluateExpression(child, ruleResultMap)) {
                        anyMatched = true;
                        break;
                    }
                }
                yield anyMatched;
            }
            case ConstraintExpression.Not notExpr -> !evaluateExpression(notExpr.child(), ruleResultMap);
        };
    }

    private RuleResource buildResource(SchedulingEngineRequest request, CompiledConstraintModel model) {
        Map<SlotKey, DemandSpec> demandMap = buildDemandMap(request.optimizationRequest().demands());
        Map<Long, Map<AvailabilityKey, DoctorAvailabilityRule>> availabilityMap = buildAvailabilityMap(request.availabilityRules());
        Map<Long, Set<TimeOffKey>> timeOffMap = buildTimeOffMap(request.timeOffRules());
        Map<Long, Map<Integer, Integer>> preferenceMap = buildPreferenceMap(request.availabilityRules());

        return new RuleResource(
                demandMap,
                availabilityMap,
                timeOffMap,
                preferenceMap,
                request.optimizationRequest().holidayDates() == null ? Set.of() : request.optimizationRequest().holidayDates(),
                request.optimizationRequest().makeupWorkdayDates() == null ? Set.of() : request.optimizationRequest().makeupWorkdayDates(),
                model
        );
    }

    private Map<SlotKey, DemandSpec> buildDemandMap(List<DepartmentScheduleDemand> demands) {
        Map<SlotKey, DemandSpec> result = new HashMap<>();
        if (demands == null) {
            return result;
        }
        for (DepartmentScheduleDemand demand : demands) {
            result.put(
                    new SlotKey(demand.date(), demand.periodCode()),
                    new DemandSpec(demand.requiredDoctors(), demand.minSeniorDoctors())
            );
        }
        return result;
    }

    private Map<Long, Map<AvailabilityKey, DoctorAvailabilityRule>> buildAvailabilityMap(List<DoctorAvailabilityRule> rules) {
        Map<Long, Map<AvailabilityKey, DoctorAvailabilityRule>> result = new HashMap<>();
        if (rules == null) {
            return result;
        }
        for (DoctorAvailabilityRule rule : rules) {
            result.computeIfAbsent(rule.doctorId(), ignored -> new HashMap<>())
                    .put(new AvailabilityKey(rule.weekday(), rule.periodCode()), rule);
        }
        return result;
    }

    private Map<Long, Map<Integer, Integer>> buildPreferenceMap(List<DoctorAvailabilityRule> rules) {
        Map<Long, Map<Integer, Integer>> result = new HashMap<>();
        if (rules == null) {
            return result;
        }
        for (DoctorAvailabilityRule rule : rules) {
            if (!rule.available()) {
                continue;
            }
            result.computeIfAbsent(rule.doctorId(), ignored -> new HashMap<>())
                    .merge(rule.periodCode(), rule.priority() == null ? 0 : rule.priority(), Integer::max);
        }
        return result;
    }

    private Map<Long, Set<TimeOffKey>> buildTimeOffMap(List<DoctorTimeOff> timeOffRules) {
        Map<Long, Set<TimeOffKey>> result = new HashMap<>();
        if (timeOffRules == null) {
            return result;
        }
        for (DoctorTimeOff timeOff : timeOffRules) {
            LocalDate cursor = timeOff.startDate();
            while (!cursor.isAfter(timeOff.endDate())) {
                result.computeIfAbsent(timeOff.doctorId(), ignored -> new HashSet<>())
                        .add(new TimeOffKey(cursor, timeOff.periodCode()));
                cursor = cursor.plusDays(1);
            }
        }
        return result;
    }

    private HolidayPolicySettings resolveHolidayPolicy(RuleResource resource, SchedulingEngineRequest request) {
        ConstraintRule policyRule = resource.model().hardRules().get("H_HOLIDAY_POLICY");
        if (policyRule == null) {
            if (request != null) {
                var hard = request.optimizationRequest().hardConstraints();
                return new HolidayPolicySettings(
                        hard.excludeHolidays(),
                        holidayPolicy(hard.holidayPolicy()),
                        normalizeReductionFactor(hard.holidayReductionFactor())
                );
            }
            return new HolidayPolicySettings(false, HolidayPolicy.REDUCED, 0.5D);
        }
        boolean exclude = boolParam(policyRule.params(), "excludeHolidays", false);
        String rawPolicy = stringParam(policyRule.params(), "policy", exclude ? "CLOSE" : "REDUCED");
        double factor = doubleParam(policyRule.params(), "reductionFactor", 0.5D);
        return new HolidayPolicySettings(exclude, holidayPolicy(rawPolicy), normalizeReductionFactor(factor));
    }

    private HolidayPolicy holidayPolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            return HolidayPolicy.REDUCED;
        }
        try {
            return HolidayPolicy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return HolidayPolicy.REDUCED;
        }
    }

    private DemandSpec adjustDemandForHoliday(
            DemandSpec demand,
            boolean effectiveHoliday,
            HolidayPolicySettings settings) {
        if (!effectiveHoliday) {
            return demand;
        }
        if (settings.excludeHolidays() || settings.policy() == HolidayPolicy.CLOSE) {
            return new DemandSpec(0, 0);
        }
        if (settings.policy() == HolidayPolicy.NORMAL) {
            return demand;
        }

        int required = Math.max(0, demand.requiredDoctors());
        if (required == 0) {
            return demand;
        }
        int reducedRequired = Math.max(1, (int) Math.floor(required * settings.reductionFactor()));
        int reducedSenior = Math.min(Math.max(0, demand.minSeniorDoctors()), reducedRequired);
        return new DemandSpec(reducedRequired, reducedSenior);
    }

    private boolean isDoctorAvailable(
            Map<Long, Map<AvailabilityKey, DoctorAvailabilityRule>> availabilityMap,
            Long doctorId,
            LocalDate date,
            Integer periodCode) {
        Map<AvailabilityKey, DoctorAvailabilityRule> map = availabilityMap.get(doctorId);
        if (map == null || map.isEmpty()) {
            return true;
        }
        DoctorAvailabilityRule rule = map.get(new AvailabilityKey(date.getDayOfWeek().getValue(), periodCode));
        if (rule == null) {
            return true;
        }
        return rule.available();
    }

    private boolean isTimeOff(
            Map<Long, Set<TimeOffKey>> timeOffMap,
            Long doctorId,
            LocalDate date,
            Integer periodCode) {
        Set<TimeOffKey> keys = timeOffMap.getOrDefault(doctorId, Set.of());
        return keys.contains(new TimeOffKey(date, periodCode)) || keys.contains(new TimeOffKey(date, null));
    }

    private boolean violatesConsecutiveDays(
            Long doctorId,
            LocalDate date,
            Map<Long, Set<LocalDate>> assignedDates,
            int maxConsecutiveDays) {
        Set<LocalDate> dates = assignedDates.getOrDefault(doctorId, Set.of());
        int consecutive = 0;
        LocalDate cursor = date.minusDays(1);
        while (dates.contains(cursor)) {
            consecutive++;
            cursor = cursor.minusDays(1);
        }
        return consecutive >= maxConsecutiveDays;
    }

    private long hoursBetween(LocalDate previousDate, Integer previousPeriodCode, LocalDate currentDate, Integer currentPeriodCode) {
        LocalDateTime previousEnd = LocalDateTime.of(previousDate, periodEnd(previousPeriodCode));
        LocalDateTime currentStart = LocalDateTime.of(currentDate, periodStart(currentPeriodCode));
        return ChronoUnit.HOURS.between(previousEnd, currentStart);
    }

    private LocalTime periodStart(Integer periodCode) {
        return switch (periodCode) {
            case 1 -> LocalTime.of(8, 0);
            case 2 -> LocalTime.of(14, 0);
            case 3 -> LocalTime.of(19, 0);
            default -> LocalTime.of(8, 0);
        };
    }

    private LocalTime periodEnd(Integer periodCode) {
        return switch (periodCode) {
            case 1 -> LocalTime.of(12, 0);
            case 2 -> LocalTime.of(18, 0);
            case 3 -> LocalTime.of(22, 0);
            default -> LocalTime.of(12, 0);
        };
    }

    private boolean isEffectiveHoliday(LocalDate date, SchedulingEngineRequest request) {
        if (request.optimizationRequest().makeupWorkdayDates() != null
                && request.optimizationRequest().makeupWorkdayDates().contains(date)) {
            return false;
        }
        return request.optimizationRequest().holidayDates() != null
                && request.optimizationRequest().holidayDates().contains(date);
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String buildUnfilledReason(Map<String, Integer> rejectionCounter) {
        if (rejectionCounter.isEmpty()) {
            return "无可用医生满足硬约束";
        }
        String details = rejectionCounter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse("UNKNOWN");
        return "无可用医生满足硬约束: " + details;
    }

    private void mergeCounter(Map<String, Integer> target, Map<String, Integer> source) {
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            target.put(entry.getKey(), target.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
    }

    private double weightedTotal(Map<String, Double> scoreBreakdown, Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return average(scoreBreakdown.values().stream().toList());
        }
        double weighted = 0D;
        double weightSum = 0D;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            double weight = entry.getValue() == null ? 0D : entry.getValue();
            weighted += scoreBreakdown.getOrDefault(entry.getKey(), 0D) * weight;
            weightSum += weight;
        }
        return weightSum <= 0D ? average(scoreBreakdown.values().stream().toList()) : weighted / weightSum;
    }

    private Map<String, Double> evaluateScores(
            List<OptimizationAssignment> assignments,
            List<OptimizationUnfilledSlot> unfilledSlots,
            List<ScheduleDoctorProfile> doctors,
            Map<SlotKey, DemandSpec> demandMap,
            Map<Long, Integer> totalShiftCounter,
            Map<Long, Integer> weekendShiftCounter,
            Map<Long, Map<Integer, Integer>> preferenceMap) {
        double fairness = 100D - variance(totalShiftCounter, doctors) * 5D;
        fairness = clamp(fairness);

        long preferenceMatched = assignments.stream()
                .filter(assignment -> preferenceMap
                        .getOrDefault(assignment.doctorId(), Map.of())
                        .getOrDefault(assignment.periodCode(), 0) > 0)
                .count();
        double preference = assignments.isEmpty() ? 0D : 100D * preferenceMatched / assignments.size();

        long continuityMatched = assignments.stream()
                .filter(assignment -> assignment.reasons().stream().anyMatch(reason -> reason.contains("连续性")))
                .count();
        double continuity = assignments.isEmpty() ? 0D : 100D * continuityMatched / assignments.size();

        long totalNeedSenior = demandMap.values().stream().mapToLong(demand -> Math.max(demand.minSeniorDoctors(), 0)).sum();
        long seniorAssigned = assignments.stream()
                .filter(assignment -> assignment.reasons().stream().anyMatch(reason -> reason.contains("资深覆盖")))
                .count();
        double seniorCoverage = totalNeedSenior <= 0 ? 100D : 100D * Math.min(seniorAssigned, totalNeedSenior) / totalNeedSenior;

        double weekendBalance = 100D - variance(weekendShiftCounter, doctors) * 10D;
        weekendBalance = clamp(weekendBalance);

        if (!unfilledSlots.isEmpty()) {
            fairness = clamp(fairness - 10D);
            preference = clamp(preference - 10D);
            continuity = clamp(continuity - 10D);
            seniorCoverage = clamp(seniorCoverage - 15D);
            weekendBalance = clamp(weekendBalance - 10D);
        }

        Map<String, Double> scores = new HashMap<>();
        scores.put("fairness", fairness);
        scores.put("preference", preference);
        scores.put("continuity", continuity);
        scores.put("seniorCoverage", seniorCoverage);
        scores.put("weekendBalance", weekendBalance);
        return scores;
    }

    private double variance(Map<Long, Integer> counter, List<ScheduleDoctorProfile> doctors) {
        if (doctors == null || doctors.isEmpty()) {
            return 0D;
        }
        double mean = doctors.stream()
                .mapToInt(doctor -> counter.getOrDefault(doctor.doctorId(), 0))
                .average()
                .orElse(0D);
        double sq = doctors.stream()
                .mapToDouble(doctor -> {
                    double delta = counter.getOrDefault(doctor.doctorId(), 0) - mean;
                    return delta * delta;
                })
                .sum();
        return sq / doctors.size();
    }

    private double clamp(double score) {
        if (score < 0D) {
            return 0D;
        }
        return Math.min(100D, score);
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0D;
        }
        double sum = 0D;
        for (Double value : values) {
            sum += value == null ? 0D : value;
        }
        return sum / values.size();
    }

    private int intParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private boolean boolParam(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }

    private double doubleParam(Map<String, Object> params, String key, double defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return defaultValue;
    }

    private String stringParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private double normalizeReductionFactor(double factor) {
        if (factor <= 0D || factor > 1D) {
            return 0.5D;
        }
        return factor;
    }

    private long deriveSeed(SchedulingEngineRequest request, String sourceHash) {
        String value = request.departmentId() + "|" + request.optimizationRequest().startDate() + "|"
                + request.optimizationRequest().endDate() + "|" + sourceHash;
        return value.hashCode();
    }

    private double tieBreaker(long baseSeed, int attemptIndex, Long doctorId, LocalDate date, Integer periodCode) {
        long mixed = baseSeed;
        mixed = 31 * mixed + attemptIndex;
        mixed = 31 * mixed + doctorId;
        mixed = 31 * mixed + date.toEpochDay();
        mixed = 31 * mixed + periodCode;
        double noise = ((mixed & 0xFFFFL) / 65535D) * 0.001D;
        return noise;
    }

    protected record SolveAttempt(
            List<OptimizationAssignment> assignments,
            List<OptimizationUnfilledSlot> unfilledSlots,
            Map<String, Double> scoreBreakdown,
            double totalScore,
            int hardViolationCount,
            List<String> warnings,
            List<String> minimalConflictSet
    ) {
    }

    protected record CandidateSelection(CandidateScore candidate, Map<String, Integer> rejectionCounter) {
    }

    protected record CandidateScore(
            ScheduleDoctorProfile doctor,
            double score,
            List<String> reasons,
            List<String> penalties
    ) {
    }

    private record CandidateReject(List<String> ruleIds) {
    }

    protected record RuleCheckResult(boolean satisfied, double penalty, String message, String code) {
        static RuleCheckResult ok(String message) {
            return new RuleCheckResult(true, 0D, message, "OK");
        }

        static RuleCheckResult ok(double score, String message) {
            return new RuleCheckResult(true, score, message, "OK");
        }

        static RuleCheckResult fail(String message, String code) {
            return new RuleCheckResult(false, -1D, message, code);
        }

        static RuleCheckResult fail(double score, String message) {
            return new RuleCheckResult(false, score, message, "FAIL");
        }
    }

    protected record RuleResource(
            Map<SlotKey, DemandSpec> demandMap,
            Map<Long, Map<AvailabilityKey, DoctorAvailabilityRule>> availabilityMap,
            Map<Long, Set<TimeOffKey>> timeOffMap,
            Map<Long, Map<Integer, Integer>> preferenceMap,
            Set<LocalDate> holidayDates,
            Set<LocalDate> makeupWorkdayDates,
            CompiledConstraintModel model
    ) {
    }

    protected record SolverState(
            Map<Long, Integer> totalShiftCounter,
            Map<DoctorWeekKey, Integer> weekShiftCounter,
            Map<Long, Integer> weekendShiftCounter,
            Map<Long, Set<LocalDate>> assignedDates,
            Set<SlotDoctorKey> assignedKeys,
            Map<Long, AssignmentPoint> latestAssignment
    ) {
        SolverState() {
            this(
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashSet<>(),
                    new HashMap<>()
            );
        }

        void markAssigned(Long doctorId, LocalDate date, Integer periodCode) {
            assignedKeys.add(new SlotDoctorKey(date, periodCode, doctorId));
            totalShiftCounter.put(doctorId, totalShiftCounter.getOrDefault(doctorId, 0) + 1);

            DoctorWeekKey weekKey = DoctorWeekKey.of(doctorId, date);
            weekShiftCounter.put(weekKey, weekShiftCounter.getOrDefault(weekKey, 0) + 1);

            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                weekendShiftCounter.put(doctorId, weekendShiftCounter.getOrDefault(doctorId, 0) + 1);
            }

            assignedDates.computeIfAbsent(doctorId, ignored -> new HashSet<>()).add(date);

            AssignmentPoint current = new AssignmentPoint(date, periodCode);
            AssignmentPoint previous = latestAssignment.get(doctorId);
            if (previous == null || current.compareTo(previous) > 0) {
                latestAssignment.put(doctorId, current);
            }
        }

        boolean containsAssignment(LocalDate date, Integer periodCode, Long doctorId) {
            return assignedKeys.contains(new SlotDoctorKey(date, periodCode, doctorId));
        }
    }

    protected record AssignmentPoint(LocalDate date, Integer periodCode) implements Comparable<AssignmentPoint> {
        @Override
        public int compareTo(AssignmentPoint other) {
            int dateCompare = this.date.compareTo(other.date);
            if (dateCompare != 0) {
                return dateCompare;
            }
            return this.periodCode.compareTo(other.periodCode);
        }
    }

    protected record SlotKey(LocalDate date, Integer periodCode) {
    }

    protected record SlotDoctorKey(LocalDate date, Integer periodCode, Long doctorId) {
    }

    protected record AvailabilityKey(Integer weekday, Integer periodCode) {
    }

    protected record TimeOffKey(LocalDate date, Integer periodCode) {
    }

    protected record DemandSpec(int requiredDoctors, int minSeniorDoctors) {
        static DemandSpec defaultSpec() {
            return new DemandSpec(1, 0);
        }
    }

    protected record DoctorWeekKey(Long doctorId, int weekBasedYear, int weekOfYear) {
        static DoctorWeekKey of(Long doctorId, LocalDate date) {
            return new DoctorWeekKey(
                    doctorId,
                    date.get(ISO_WEEK.weekBasedYear()),
                    date.get(ISO_WEEK.weekOfWeekBasedYear())
            );
        }
    }

    protected record HolidayPolicySettings(boolean excludeHolidays, HolidayPolicy policy, double reductionFactor) {
    }

    protected enum HolidayPolicy {
        CLOSE,
        REDUCED,
        NORMAL
    }
}
