package me.jianwen.mediask.schedule.domain.optimization.service;

import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationResult;
import me.jianwen.mediask.schedule.domain.rule.DoctorAvailabilityRule;
import me.jianwen.mediask.schedule.domain.rule.DoctorTimeOff;
import me.jianwen.mediask.schedule.domain.optimization.OptimizationAssignment;
import me.jianwen.mediask.schedule.domain.optimization.OptimizationUnfilledSlot;
import me.jianwen.mediask.schedule.domain.optimization.ScheduleDoctorProfile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 科室多医生联合自动排班（规则+评分优化）
 */
public class DepartmentScheduleOptimizationDomainService {

    public DepartmentScheduleOptimizationResult optimize(
            DepartmentScheduleOptimizationRequest request,
            List<ScheduleDoctorProfile> doctors,
            List<DoctorAvailabilityRule> availabilityRules,
            List<DoctorTimeOff> timeOffRules) {

        List<OptimizationAssignment> assignments = new ArrayList<>();
        List<OptimizationUnfilledSlot> unfilledSlots = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (doctors == null || doctors.isEmpty()) {
            warnings.add("未找到可参与排班的医生");
            return new DepartmentScheduleOptimizationResult(
                    assignments,
                    List.of(new OptimizationUnfilledSlot(request.startDate(), null, 0, "医生池为空")),
                    Map.of("fairness", 0D, "preference", 0D, "continuity", 0D, "seniorCoverage", 0D, "weekendBalance", 0D),
                    0D,
                    1,
                    warnings
            );
        }

        Map<SlotKey, DemandSpec> demandMap = buildDemandMap(request);
        Map<SlotKey, DemandSpec> effectiveDemandMap = new HashMap<>();
        Set<SlotDoctorKey> assignedKeys = new HashSet<>();
        Map<Long, Integer> weeklyShiftCounter = new HashMap<>();
        Map<Long, Integer> weekendShiftCounter = new HashMap<>();
        Map<Long, Set<LocalDate>> doctorAssignedDates = new HashMap<>();
        Map<Long, Set<Integer>> doctorPreferredPeriods = buildPreferredPeriods(availabilityRules);
        Map<Long, Set<TimeOffKey>> doctorTimeOff = buildTimeOffMap(timeOffRules);

        LocalDate cursor = request.startDate();
        while (!cursor.isAfter(request.endDate())) {
            HolidayPolicy holidayPolicy = resolveHolidayPolicy(request);
            if (isEffectiveHoliday(cursor, request) && holidayPolicy == HolidayPolicy.CLOSE) {
                cursor = cursor.plusDays(1);
                continue;
            }
            for (Integer periodCode : request.periods()) {
                SlotKey slotKey = new SlotKey(cursor, periodCode);
                DemandSpec baseDemand = demandMap.getOrDefault(slotKey, DemandSpec.defaultSpec());
                DemandSpec demand = adjustDemandForHoliday(baseDemand, cursor, request, holidayPolicy);
                effectiveDemandMap.put(slotKey, demand);
                if (demand.requiredDoctors() <= 0) {
                    continue;
                }

                List<Long> pickedDoctors = new ArrayList<>();
                int neededSenior = Math.max(demand.minSeniorDoctors(), 0);
                for (int i = 0; i < demand.requiredDoctors(); i++) {
                    Candidate candidate = selectBestCandidate(
                            cursor,
                            periodCode,
                            doctors,
                            doctorTimeOff,
                            assignedKeys,
                            pickedDoctors,
                            weeklyShiftCounter,
                            weekendShiftCounter,
                            doctorAssignedDates,
                            doctorPreferredPeriods,
                            neededSenior,
                            request
                    );

                    if (candidate == null) {
                        int missing = demand.requiredDoctors() - i;
                        unfilledSlots.add(new OptimizationUnfilledSlot(
                                cursor,
                                periodCode,
                                missing,
                                "无可用医生满足硬约束"
                        ));
                        break;
                    }

                    pickedDoctors.add(candidate.doctor().doctorId());
                    if (candidate.doctor().senior() && neededSenior > 0) {
                        neededSenior--;
                    }
                    assignments.add(new OptimizationAssignment(
                            cursor,
                            periodCode,
                            candidate.doctor().doctorId(),
                            candidate.reasons(),
                            candidate.penalties()
                    ));
                    assignedKeys.add(new SlotDoctorKey(cursor, periodCode, candidate.doctor().doctorId()));
                    increment(weeklyShiftCounter, candidate.doctor().doctorId());
                    if (isWeekend(cursor)) {
                        increment(weekendShiftCounter, candidate.doctor().doctorId());
                    }
                    doctorAssignedDates.computeIfAbsent(candidate.doctor().doctorId(), ignored -> new HashSet<>()).add(cursor);
                }
            }
            cursor = cursor.plusDays(1);
        }

        Map<String, Double> scoreBreakdown = evaluateScores(
                assignments,
                unfilledSlots,
                doctors,
                effectiveDemandMap,
                weeklyShiftCounter,
                weekendShiftCounter,
                doctorPreferredPeriods
        );
        double totalScore = weightedTotal(scoreBreakdown, request.softGoals());
        int hardViolationCount = unfilledSlots.stream().mapToInt(OptimizationUnfilledSlot::missingDoctors).sum();
        if (hardViolationCount > 0) {
            warnings.add("存在未满足的排班需求，请根据未排满时段进行人工补排");
        }

        return new DepartmentScheduleOptimizationResult(
                assignments,
                unfilledSlots,
                scoreBreakdown,
                totalScore,
                hardViolationCount,
                warnings
        );
    }

    private Candidate selectBestCandidate(
            LocalDate date,
            Integer periodCode,
            List<ScheduleDoctorProfile> doctors,
            Map<Long, Set<TimeOffKey>> doctorTimeOff,
            Set<SlotDoctorKey> assignedKeys,
            List<Long> pickedDoctors,
            Map<Long, Integer> weeklyShiftCounter,
            Map<Long, Integer> weekendShiftCounter,
            Map<Long, Set<LocalDate>> doctorAssignedDates,
            Map<Long, Set<Integer>> doctorPreferredPeriods,
            int neededSenior,
            DepartmentScheduleOptimizationRequest request) {

        List<Candidate> candidates = new ArrayList<>();
        for (ScheduleDoctorProfile doctor : doctors) {
            if (pickedDoctors.contains(doctor.doctorId())) {
                continue;
            }
            if (assignedKeys.contains(new SlotDoctorKey(date, periodCode, doctor.doctorId()))) {
                continue;
            }
            if (isTimeOff(doctorTimeOff, doctor.doctorId(), date, periodCode)) {
                continue;
            }
            if (violatesConsecutiveDays(doctor.doctorId(), date, doctorAssignedDates, request.hardConstraints().maxConsecutiveDays())) {
                continue;
            }
            int weekShifts = weeklyShiftCounter.getOrDefault(doctor.doctorId(), 0);
            if (weekShifts >= request.hardConstraints().maxShiftsPerWeek()) {
                continue;
            }

            List<String> reasons = new ArrayList<>();
            List<String> penalties = new ArrayList<>();
            double score = 0D;

            score += scoreFairness(weekShifts, reasons);
            score += scorePreference(doctor.doctorId(), periodCode, doctorPreferredPeriods, reasons, penalties);
            score += scoreContinuity(doctor.doctorId(), date, periodCode, doctorAssignedDates, reasons);
            score += scoreSeniorCoverage(doctor, neededSenior, reasons, penalties);
            score += scoreWeekendBalance(date, doctor.doctorId(), weekendShiftCounter, reasons, penalties);

            candidates.add(new Candidate(doctor, score, reasons, penalties));
        }

        return candidates.stream()
                .max(Comparator.comparingDouble(Candidate::score))
                .orElse(null);
    }

    private double scoreFairness(int weekShifts, List<String> reasons) {
        double score = Math.max(0D, 5D - weekShifts * 0.5D);
        reasons.add("公平性加分: 当前周班次=" + weekShifts);
        return score;
    }

    private double scorePreference(
            Long doctorId,
            Integer periodCode,
            Map<Long, Set<Integer>> doctorPreferredPeriods,
            List<String> reasons,
            List<String> penalties) {
        Set<Integer> preferred = doctorPreferredPeriods.getOrDefault(doctorId, Set.of());
        if (preferred.isEmpty() || preferred.contains(periodCode)) {
            reasons.add("偏好匹配: 时段命中");
            return 3D;
        }
        penalties.add("偏好惩罚: 非偏好时段");
        return -2D;
    }

    private double scoreContinuity(Long doctorId, LocalDate date, Integer periodCode,
                                   Map<Long, Set<LocalDate>> doctorAssignedDates, List<String> reasons) {
        Set<LocalDate> dates = doctorAssignedDates.getOrDefault(doctorId, Set.of());
        if (dates.contains(date.minusDays(1))) {
            reasons.add("连续性加分: 与前一日排班衔接");
            return 1.5D;
        }
        return 0.5D;
    }

    private double scoreSeniorCoverage(
            ScheduleDoctorProfile doctor,
            int neededSenior,
            List<String> reasons,
            List<String> penalties) {
        if (neededSenior <= 0) {
            return 0.5D;
        }
        if (doctor.senior()) {
            reasons.add("专家覆盖加分: 满足资深医生需求");
            return 4D;
        }
        penalties.add("专家覆盖惩罚: 资深医生仍有缺口");
        return -3D;
    }

    private double scoreWeekendBalance(
            LocalDate date,
            Long doctorId,
            Map<Long, Integer> weekendShiftCounter,
            List<String> reasons,
            List<String> penalties) {
        if (!isWeekend(date)) {
            return 0D;
        }
        int weekendShifts = weekendShiftCounter.getOrDefault(doctorId, 0);
        if (weekendShifts <= 1) {
            reasons.add("周末均衡加分: 周末班次较少");
            return 2D;
        }
        penalties.add("周末均衡惩罚: 周末班次偏多");
        return -1.5D;
    }

    private Map<SlotKey, DemandSpec> buildDemandMap(DepartmentScheduleOptimizationRequest request) {
        Map<SlotKey, DemandSpec> result = new HashMap<>();
        if (request.demands() == null || request.demands().isEmpty()) {
            return result;
        }
        for (DepartmentScheduleDemand demand : request.demands()) {
            result.put(
                    new SlotKey(demand.date(), demand.periodCode()),
                    new DemandSpec(demand.requiredDoctors(), demand.minSeniorDoctors())
            );
        }
        return result;
    }

    private Map<Long, Set<Integer>> buildPreferredPeriods(List<DoctorAvailabilityRule> rules) {
        Map<Long, Set<Integer>> result = new HashMap<>();
        for (DoctorAvailabilityRule rule : rules) {
            if (!rule.available()) {
                continue;
            }
            result.computeIfAbsent(rule.doctorId(), ignored -> new HashSet<>()).add(rule.periodCode());
        }
        return result;
    }

    private Map<Long, Set<TimeOffKey>> buildTimeOffMap(List<DoctorTimeOff> timeOffs) {
        Map<Long, Set<TimeOffKey>> result = new HashMap<>();
        for (DoctorTimeOff timeOff : timeOffs) {
            LocalDate cursor = timeOff.startDate();
            while (!cursor.isAfter(timeOff.endDate())) {
                result.computeIfAbsent(timeOff.doctorId(), ignored -> new HashSet<>())
                        .add(new TimeOffKey(cursor, timeOff.periodCode()));
                cursor = cursor.plusDays(1);
            }
        }
        return result;
    }

    private boolean isTimeOff(Map<Long, Set<TimeOffKey>> timeOffMap, Long doctorId, LocalDate date, Integer periodCode) {
        Set<TimeOffKey> keys = timeOffMap.getOrDefault(doctorId, Set.of());
        return keys.contains(new TimeOffKey(date, periodCode)) || keys.contains(new TimeOffKey(date, null));
    }

    private boolean violatesConsecutiveDays(Long doctorId, LocalDate date, Map<Long, Set<LocalDate>> assignedDates, int maxConsecutiveDays) {
        Set<LocalDate> dates = assignedDates.getOrDefault(doctorId, Set.of());
        int consecutive = 0;
        LocalDate cursor = date.minusDays(1);
        while (dates.contains(cursor)) {
            consecutive++;
            cursor = cursor.minusDays(1);
        }
        return consecutive >= maxConsecutiveDays;
    }

    private Map<String, Double> evaluateScores(
            List<OptimizationAssignment> assignments,
            List<OptimizationUnfilledSlot> unfilledSlots,
            List<ScheduleDoctorProfile> doctors,
            Map<SlotKey, DemandSpec> demandMap,
            Map<Long, Integer> weeklyShiftCounter,
            Map<Long, Integer> weekendShiftCounter,
            Map<Long, Set<Integer>> doctorPreferredPeriods) {

        double fairness = 100D - variance(weeklyShiftCounter, doctors) * 5D;
        fairness = clamp(fairness);

        long preferenceMatched = assignments.stream()
                .filter(a -> doctorPreferredPeriods.getOrDefault(a.doctorId(), Set.of()).contains(a.periodCode()))
                .count();
        double preference = assignments.isEmpty() ? 0D : 100D * preferenceMatched / assignments.size();

        long continuityMatched = assignments.stream()
                .filter(a -> a.reasons().stream().anyMatch(reason -> reason.contains("连续性")))
                .count();
        double continuity = assignments.isEmpty() ? 0D : 100D * continuityMatched / assignments.size();

        long totalNeedSenior = demandMap.values().stream().mapToLong(d -> Math.max(d.minSeniorDoctors(), 0)).sum();
        long seniorAssigned = assignments.stream()
                .filter(a -> a.reasons().stream().anyMatch(reason -> reason.contains("专家覆盖加分")))
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

    private double weightedTotal(Map<String, Double> scores, DepartmentScheduleOptimizationRequest.SoftGoals goals) {
        double weightSum = goals.fairnessWeight()
                + goals.preferenceWeight()
                + goals.continuityWeight()
                + goals.seniorCoverageWeight()
                + goals.weekendBalanceWeight();
        if (weightSum <= 0D) {
            return 0D;
        }
        return (
                scores.getOrDefault("fairness", 0D) * goals.fairnessWeight()
                        + scores.getOrDefault("preference", 0D) * goals.preferenceWeight()
                        + scores.getOrDefault("continuity", 0D) * goals.continuityWeight()
                        + scores.getOrDefault("seniorCoverage", 0D) * goals.seniorCoverageWeight()
                        + scores.getOrDefault("weekendBalance", 0D) * goals.weekendBalanceWeight()
        ) / weightSum;
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

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private HolidayPolicy resolveHolidayPolicy(DepartmentScheduleOptimizationRequest request) {
        if (request.hardConstraints().excludeHolidays()) {
            return HolidayPolicy.CLOSE;
        }
        String rawPolicy = request.hardConstraints().holidayPolicy();
        if (rawPolicy == null || rawPolicy.isBlank()) {
            return HolidayPolicy.REDUCED;
        }
        try {
            return HolidayPolicy.valueOf(rawPolicy.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return HolidayPolicy.REDUCED;
        }
    }

    private DemandSpec adjustDemandForHoliday(
            DemandSpec demand,
            LocalDate date,
            DepartmentScheduleOptimizationRequest request,
            HolidayPolicy policy) {
        if (!isEffectiveHoliday(date, request)) {
            return demand;
        }
        if (policy == HolidayPolicy.NORMAL) {
            return demand;
        }
        if (policy == HolidayPolicy.CLOSE) {
            return new DemandSpec(0, 0);
        }
        int required = Math.max(demand.requiredDoctors(), 0);
        if (required == 0) {
            return demand;
        }
        double factor = normalizeReductionFactor(request.hardConstraints().holidayReductionFactor());
        int reducedRequired = Math.max(1, (int) Math.floor(required * factor));
        int reducedSenior = Math.min(Math.max(demand.minSeniorDoctors(), 0), reducedRequired);
        return new DemandSpec(reducedRequired, reducedSenior);
    }

    private double normalizeReductionFactor(double factor) {
        if (factor <= 0D || factor > 1D) {
            return 0.5D;
        }
        return factor;
    }

    private boolean isEffectiveHoliday(LocalDate date, DepartmentScheduleOptimizationRequest request) {
        if (request.makeupWorkdayDates() != null && request.makeupWorkdayDates().contains(date)) {
            return false;
        }
        return request.holidayDates() != null && request.holidayDates().contains(date);
    }

    private double clamp(double score) {
        if (score < 0D) {
            return 0D;
        }
        return Math.min(100D, score);
    }

    private void increment(Map<Long, Integer> map, Long key) {
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private record SlotKey(LocalDate date, Integer periodCode) {
    }

    private record SlotDoctorKey(LocalDate date, Integer periodCode, Long doctorId) {
    }

    private record TimeOffKey(LocalDate date, Integer periodCode) {
    }

    private record DemandSpec(int requiredDoctors, int minSeniorDoctors) {
        private static DemandSpec defaultSpec() {
            return new DemandSpec(1, 0);
        }
    }

    private enum HolidayPolicy {
        CLOSE,
        REDUCED,
        NORMAL
    }

    private record Candidate(
            ScheduleDoctorProfile doctor,
            double score,
            List<String> reasons,
            List<String> penalties
    ) {
    }
}
