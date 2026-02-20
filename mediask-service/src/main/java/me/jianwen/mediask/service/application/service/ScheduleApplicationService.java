package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.dto.schedule.AutoSchedulePlanDTO;
import me.jianwen.mediask.common.dto.schedule.ScheduleDTO;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.model.PageResult;
import me.jianwen.mediask.common.util.JsonUtil;
import me.jianwen.mediask.domain.event.DomainEventPublisher;
import me.jianwen.mediask.domain.repository.DoctorRepository;
import me.jianwen.mediask.schedule.domain.engine.SchedulingEngine;
import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineResult;
import me.jianwen.mediask.schedule.domain.engine.SolverConfig;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationResult;
import me.jianwen.mediask.schedule.domain.optimization.model.CalendarDayRule;
import me.jianwen.mediask.schedule.domain.optimization.model.DoctorAvailabilityRule;
import me.jianwen.mediask.schedule.domain.optimization.model.DoctorTimeOff;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleDoctorProfile;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleRuleProfile;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlan;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlanConstraintSnapshot;
import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlanItem;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.repository.CalendarDayRepository;
import me.jianwen.mediask.schedule.domain.repository.DepartmentScheduleDemandRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorAvailabilityRuleRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorTimeOffRepository;
import me.jianwen.mediask.schedule.domain.repository.SchedulePlanRepository;
import me.jianwen.mediask.schedule.domain.repository.ScheduleRuleProfileRepository;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import me.jianwen.mediask.service.application.command.AutoScheduleCommand;
import me.jianwen.mediask.service.application.command.CreateScheduleCommand;
import me.jianwen.mediask.service.application.support.SchedulePlanDiffCalculator;
import me.jianwen.mediask.user.domain.entity.DoctorProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 排班应用服务
 *
 * 职责：
 * 1. 协调多个聚合和领域服务完成业务用例
 * 2. 管理事务边界
 * 3. 发布领域事件
 * 4. Request 到领域对象的转换
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleApplicationService {

    private final DoctorScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotManagementDomainService slotManagementDomainService;
    private final SchedulingEngine schedulingEngine;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRuleRepository doctorAvailabilityRuleRepository;
    private final DoctorTimeOffRepository doctorTimeOffRepository;
    private final DepartmentScheduleDemandRepository departmentScheduleDemandRepository;
    private final SchedulePlanRepository schedulePlanRepository;
    private final ScheduleRuleProfileRepository scheduleRuleProfileRepository;
    private final CalendarDayRepository calendarDayRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * 创建单个排班
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(CreateScheduleCommand request) {
        log.info("创建排班: doctorId={}, date={}, period={}",
                request.getDoctorId(), request.getScheduleDate(), request.getTimePeriodCode());

        DoctorId doctorId = DoctorId.of(request.getDoctorId());
        TimePeriod timePeriod = resolveTimePeriod(request.getTimePeriodCode());

        // 1. 检查是否已存在排班
        if (scheduleRepository.exists(doctorId, request.getScheduleDate(), timePeriod)) {
            throw new BizException(ErrorCode.OPERATION_FORBIDDEN, "该时段的排班已存在");
        }

        // 2. 创建排班聚合
        DoctorSchedule schedule = DoctorSchedule.create(
                ScheduleId.generate(),
                doctorId,
                request.getScheduleDate(),
                timePeriod,
                request.getTotalSlots(),
                request.getSlotDurationMinutes());

        // 3. 保存排班
        scheduleRepository.save(schedule);

        // 4. 生成号源时段
        List<AppointmentSlot> slots = slotManagementDomainService.generateSlotsForSchedule(schedule);
        slotManagementDomainService.saveSlots(slots);

        // 5. 发布领域事件
        publishEvents(schedule);

        log.info("排班创建成功: scheduleId={}", schedule.getId());

        return schedule.getId().getValue();
    }

    /**
     * 执行自动排班
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoSchedulePlanDTO autoSchedule(AutoScheduleCommand request) {
        validateAutoScheduleCommand(request);
        LocalDate planningStartDate = request.resolvedPlanningStartDate();
        LocalDate planningEndDate = request.resolvedPlanningEndDate();
        SchedulePlan basePlan = request.incrementalReplan() ? resolveBasePlan(request) : null;
        log.info("执行自动排班: departmentId={}, dateRange={} to {}, doctorIds={}",
                request.getDepartmentId(), planningStartDate, planningEndDate, request.getDoctorIds());

        List<DoctorProfile> doctorProfiles = doctorRepository.listActiveByDepartment(
                request.getDepartmentId(), request.getDoctorIds());
        if (doctorProfiles.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "未找到可参与排班的在职医生");
        }

        List<ScheduleDoctorProfile> doctors = doctorProfiles.stream()
                .map(this::toScheduleDoctorProfile)
                .toList();
        List<Long> doctorIds = doctors.stream().map(ScheduleDoctorProfile::doctorId).toList();

        List<DoctorAvailabilityRule> availabilityRules = doctorAvailabilityRuleRepository.listByDoctorIds(doctorIds);
        List<DoctorTimeOff> timeOffRules = doctorTimeOffRepository.listByDoctorIdsAndDateRange(
                doctorIds, planningStartDate, planningEndDate);
        List<DepartmentScheduleDemand> demands = resolveDemands(request, planningStartDate, planningEndDate);
        List<CalendarDayRule> calendarDays = calendarDayRepository.listByDateRange(
                planningStartDate, planningEndDate, "CN-NATIONAL");
        Set<LocalDate> holidayDates = calendarDays.stream()
                .filter(CalendarDayRule::holiday)
                .map(CalendarDayRule::date)
                .collect(Collectors.toSet());
        Set<LocalDate> makeupWorkdayDates = calendarDays.stream()
                .filter(CalendarDayRule::makeupWorkday)
                .map(CalendarDayRule::date)
                .collect(Collectors.toSet());

        DepartmentScheduleOptimizationRequest optimizationRequest = new DepartmentScheduleOptimizationRequest(
                planningStartDate,
                planningEndDate,
                request.getPeriods(),
                demands,
                holidayDates,
                makeupWorkdayDates,
                toHardConstraints(request.resolvedHardConstraints()),
                toSoftGoals(request.resolvedSoftGoals())
        );
        String resolvedConstraintDslJson = resolveConstraintDslJson(request);
        AutoScheduleCommand.SolverConfig solverConfig = request.resolvedSolverConfig();
        SchedulingEngineResult engineResult = schedulingEngine.optimize(new SchedulingEngineRequest(
                "DEFAULT",
                request.getDepartmentId(),
                optimizationRequest,
                doctors,
                availabilityRules,
                timeOffRules,
                new SolverConfig(
                        SolverStrategy.from(solverConfig.strategy()),
                        solverConfig.maxIterations(),
                        solverConfig.timeLimitMs(),
                        solverConfig.seed()
                ),
                resolvedConstraintDslJson
        ));
        DepartmentScheduleOptimizationResult optimizationResult = engineResult.result();

        List<Long> generatedScheduleIds = new ArrayList<>();
        List<String> warnings = new ArrayList<>(engineResult.warnings());
        if (request.incrementalReplan()) {
            warnings.add("本次执行增量重排: basePlanId=%d, window=%s~%s"
                    .formatted(request.getBasePlanId(), planningStartDate, planningEndDate));
        }
        for (var assignment : optimizationResult.assignments()) {
            TimePeriod period = resolveTimePeriod(assignment.periodCode());
            DoctorId doctorId = DoctorId.of(assignment.doctorId());
            if (scheduleRepository.exists(doctorId, assignment.date(), period)) {
                warnings.add("跳过已存在排班: doctorId=%d,date=%s,period=%d"
                        .formatted(assignment.doctorId(), assignment.date(), assignment.periodCode()));
                continue;
            }
        }

        List<SchedulePlanItem> planItems;
        SchedulePlanDiffCalculator.PlanDiffResult planDiffResult = null;
        if (request.incrementalReplan()) {
            List<SchedulePlanItem> basePlanItems = schedulePlanRepository.listPlanItems(basePlan.id());
            Set<Long> affectedDoctorIds = resolveAffectedDoctorIds(request, doctors);
            planItems = mergeIncrementalPlanItems(
                    basePlanItems,
                    optimizationResult,
                    doctors,
                    affectedDoctorIds,
                    planningStartDate,
                    planningEndDate
            );
            planDiffResult = SchedulePlanDiffCalculator.calculate(basePlanItems, planItems);
            warnings.add("增量重排差异: added=%d, removed=%d, changedSlots=%d"
                    .formatted(
                            planDiffResult.addedAssignments(),
                            planDiffResult.removedAssignments(),
                            planDiffResult.changedSlots()
                    ));
        } else {
            planItems = buildPlanItems(optimizationResult, doctors);
        }

        String planCode = basePlan != null
                ? basePlan.planCode()
                : buildPlanCode(request.getDepartmentId(), request.getStartDate(), request.getEndDate());
        int versionNo = schedulePlanRepository.findLatestVersion(planCode) + 1;
        Long planId = schedulePlanRepository.savePlan(new SchedulePlan(
                null,
                planCode,
                request.getDepartmentId(),
                basePlan != null ? basePlan.startDate() : request.getStartDate(),
                basePlan != null ? basePlan.endDate() : request.getEndDate(),
                versionNo,
                "DRAFT",
                engineResult.actualStrategy().name(),
                null,
                optimizationResult.totalScore(),
                optimizationResult.hardViolationCount(),
                JsonUtil.toJson(warnings)
        ));
        schedulePlanRepository.savePlanItems(planId, planItems);
        schedulePlanRepository.saveConstraintSnapshots(planId, buildSnapshots(
                request,
                engineResult,
                resolvedConstraintDslJson,
                basePlan,
                availabilityRules,
                timeOffRules,
                demands,
                holidayDates,
                makeupWorkdayDates
        ));
        warnings.add("方案已保存为DRAFT，需调用发布接口后才会生效");

        log.info("自动排班完成: 生成 {} 条排班, 未满足时段={}",
                generatedScheduleIds.size(), optimizationResult.unfilledSlots().size());
        return toPlanDTO(
                generatedScheduleIds,
                optimizationResult,
                engineResult.minimalConflictSet(),
                toPlanDiffDTO(planDiffResult),
                warnings,
                planCode + "@v" + versionNo
        );
    }

    /**
     * 停诊（医生请假）
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeSchedule(Long scheduleId, String reason) {
        log.info("停诊: scheduleId={}, reason={}", scheduleId, reason);

        DoctorSchedule schedule = getScheduleEntityById(scheduleId);
        schedule.close(reason);
        scheduleRepository.save(schedule);

        publishEvents(schedule);

        log.info("停诊成功: scheduleId={}", scheduleId);
    }

    /**
     * 开诊（取消停诊）
     */
    @Transactional(rollbackFor = Exception.class)
    public void openSchedule(Long scheduleId) {
        log.info("开诊: scheduleId={}", scheduleId);

        DoctorSchedule schedule = getScheduleEntityById(scheduleId);
        schedule.open();
        scheduleRepository.save(schedule);

        publishEvents(schedule);

        log.info("开诊成功: scheduleId={}", scheduleId);
    }

    /**
     * 调整号源数量
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustTotalSlots(Long scheduleId, int newTotalSlots) {
        log.info("调整号源: scheduleId={}, newTotalSlots={}", scheduleId, newTotalSlots);

        DoctorSchedule schedule = getScheduleEntityById(scheduleId);
        schedule.adjustTotalSlots(newTotalSlots);
        scheduleRepository.save(schedule);

        // TODO: 同步调整时段数量

        publishEvents(schedule);

        log.info("号源调整成功: scheduleId={}", scheduleId);
    }

    /**
     * 查询排班详情
     */
    public ScheduleDTO getScheduleById(Long scheduleId) {
        return toScheduleDTO(getScheduleEntityById(scheduleId));
    }

    /**
     * 查询医生在日期范围内的排班
     */
    public List<ScheduleDTO> listSchedulesByDoctorAndDateRange(Long doctorId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByDoctorAndDateRange(DoctorId.of(doctorId), startDate, endDate)
                .stream().map(this::toScheduleDTO).toList();
    }

    /**
     * 查询可预约的排班
     */
    public List<ScheduleDTO> listOpenSchedules(LocalDate date, Integer periodCode) {
        TimePeriod period = resolveTimePeriod(periodCode);
        return scheduleRepository.findOpenSchedulesByDateAndPeriod(date, period)
                .stream().map(this::toScheduleDTO).toList();
    }

    /**
     * 查询可预约的排班（按科室筛选）
     */
    public List<ScheduleDTO> listOpenSchedulesByDepartment(LocalDate date, Integer periodCode, Long departmentId) {
        return listOpenSchedules(date, periodCode);
    }

    /**
     * 分页查询排班列表
     */
    public PageResult<ScheduleDTO> listSchedulesPaged(
            Long doctorId, Long departmentId, LocalDate startDate, LocalDate endDate,
            Integer status, Integer pageNum, Integer pageSize) {

        LocalDate queryStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(1);
        LocalDate queryEndDate = endDate != null ? endDate : LocalDate.now().plusMonths(1);
        List<DoctorSchedule> allSchedules = doctorId != null
                ? scheduleRepository.findByDoctorAndDateRange(DoctorId.of(doctorId), queryStartDate, queryEndDate)
                : scheduleRepository.findByDateRange(queryStartDate, queryEndDate);

        // 过滤条件
        if (status != null) {
            allSchedules = allSchedules.stream()
                    .filter(s -> s.getStatus().getCode().equals(status))
                    .toList();
        }

        // 分页
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, allSchedules.size());
        long total = allSchedules.size();

        if (start >= allSchedules.size()) {
            return new PageResult<>(total, pageNum, pageSize, List.of());
        }

        List<ScheduleDTO> pageSchedules = allSchedules.subList(start, end).stream()
                .map(this::toScheduleDTO)
                .toList();
        return new PageResult<>(total, pageNum, pageSize, pageSchedules);
    }

    /**
     * 逻辑删除排班（检查关联预约）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long scheduleId, boolean force) {
        log.info("删除排班: scheduleId={}, force={}", scheduleId, force);

        DoctorSchedule schedule = getScheduleEntityById(scheduleId);

        // 检查是否有未取消的预约
        List<Appointment> appointments = appointmentRepository.findByScheduleId(schedule.getId());
        long activeAppointments = appointments.stream()
                .filter(a -> !a.isCancelled())
                .count();

        if (activeAppointments > 0 && !force) {
            throw new BizException(ErrorCode.OPERATION_FORBIDDEN,
                    "该排班存在 " + activeAppointments + " 个未取消的预约，请先取消预约后再删除");
        }

        // 如果强制删除，取消所有预约
        if (activeAppointments > 0 && force) {
            appointments.stream()
                    .filter(a -> !a.isCancelled())
                    .forEach(a -> {
                        a.cancel("排班已删除");
                        appointmentRepository.save(a);
                    });
            log.info("已取消 {} 个关联预约", activeAppointments);
        }

        // 逻辑删除：标记为已过期或已停诊
        schedule.close("排班已删除");
        scheduleRepository.save(schedule);

        publishEvents(schedule);

        log.info("排班删除成功: scheduleId={}", scheduleId);
    }

    /**
     * 批量删除排班（按日期范围）
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteSchedules(Long doctorId, LocalDate startDate, LocalDate endDate, boolean force) {
        log.info("批量删除排班: doctorId={}, dateRange={} to {}", doctorId, startDate, endDate);

        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorAndDateRange(
                DoctorId.of(doctorId), startDate, endDate);

        int deletedCount = 0;
        for (DoctorSchedule schedule : schedules) {
            try {
                deleteSchedule(schedule.getId().getValue(), force);
                deletedCount++;
            } catch (BizException e) {
                log.warn("排班删除失败: scheduleId={}, reason={}",
                        schedule.getId().getValue(), e.getMessage());
            }
        }

        log.info("批量删除完成: 共处理 {} 条，成功 {} 条", schedules.size(), deletedCount);
        return deletedCount;
    }

    /**
     * 发布领域事件
     */
    private void publishEvents(DoctorSchedule schedule) {
        schedule.getDomainEvents().forEach(event -> {
            if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleCreatedEvent createdEvent) {
                eventPublisher.publishScheduleCreated(createdEvent);
            } else if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleSlotDecreasedEvent decreasedEvent) {
                eventPublisher.publishScheduleSlotDecreased(decreasedEvent);
            } else if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleSlotIncreasedEvent increasedEvent) {
                eventPublisher.publishScheduleSlotIncreased(increasedEvent);
            } else if (event instanceof me.jianwen.mediask.schedule.domain.event.ScheduleStatusChangedEvent statusEvent) {
                eventPublisher.publishScheduleStatusChanged(statusEvent);
            } else {
                log.warn("未处理的领域事件类型: {}", event.getClass().getName());
            }
        });
        schedule.clearDomainEvents();
    }

    private DoctorSchedule getScheduleEntityById(Long scheduleId) {
        return scheduleRepository.findById(ScheduleId.of(scheduleId))
                .orElseThrow(() -> new BizException(ErrorCode.SCHEDULE_NOT_FOUND, "排班不存在: " + scheduleId));
    }

    private TimePeriod resolveTimePeriod(Integer periodCode) {
        try {
            return TimePeriod.fromCode(periodCode);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法的时段编码");
        }
    }

    private void validateAutoScheduleCommand(AutoScheduleCommand request) {
        if (request.getDepartmentId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "科室ID不能为空");
        }
        if (request.getPeriods() == null || request.getPeriods().isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "时段列表不能为空");
        }
        if (!request.incrementalReplan()) {
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "排班日期范围不能为空");
            }
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "开始日期不能晚于结束日期");
            }
        }
        if (request.incrementalReplan()) {
            if (request.getReplanWindowStartDate() == null || request.getReplanWindowEndDate() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "增量重排必须指定 replanWindow.startDate/endDate");
            }
            if (request.getReplanWindowStartDate().isAfter(request.getReplanWindowEndDate())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "增量重排窗口开始日期不能晚于结束日期");
            }
        }
    }

    private ScheduleDoctorProfile toScheduleDoctorProfile(DoctorProfile profile) {
        return new ScheduleDoctorProfile(
                profile.getDoctorId(),
                profile.getDepartmentId(),
                profile.getTitle(),
                isSeniorTitle(profile.getTitle())
        );
    }

    private boolean isSeniorTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        return normalized.contains("主任") || normalized.contains("教授") || normalized.contains("expert");
    }

    private List<DepartmentScheduleDemand> resolveDemands(
            AutoScheduleCommand request,
            LocalDate planningStartDate,
            LocalDate planningEndDate) {
        if (request.getDemands() != null && !request.getDemands().isEmpty()) {
            return request.getDemands().stream()
                    .filter(item -> !item.date().isBefore(planningStartDate) && !item.date().isAfter(planningEndDate))
                    .map(item -> new DepartmentScheduleDemand(
                            request.getDepartmentId(),
                            item.date(),
                            item.periodCode(),
                            item.requiredDoctors(),
                            item.minSeniorDoctors() == null ? 0 : item.minSeniorDoctors()
                    ))
                    .toList();
        }
        return departmentScheduleDemandRepository.listByDepartmentAndDateRange(
                request.getDepartmentId(), planningStartDate, planningEndDate);
    }

    private DepartmentScheduleOptimizationRequest.HardConstraints toHardConstraints(AutoScheduleCommand.HardConstraints source) {
        return new DepartmentScheduleOptimizationRequest.HardConstraints(
                source.maxConsecutiveDays(),
                source.maxShiftsPerWeek(),
                source.minRestHoursBetweenShifts(),
                source.excludeHolidays(),
                source.holidayPolicy(),
                source.holidayReductionFactor()
        );
    }

    private DepartmentScheduleOptimizationRequest.SoftGoals toSoftGoals(AutoScheduleCommand.SoftGoals source) {
        return new DepartmentScheduleOptimizationRequest.SoftGoals(
                source.fairnessWeight(),
                source.preferenceWeight(),
                source.continuityWeight(),
                source.seniorCoverageWeight(),
                source.weekendBalanceWeight()
        );
    }

    private AutoSchedulePlanDTO toPlanDTO(
            List<Long> generatedScheduleIds,
            DepartmentScheduleOptimizationResult result,
            List<String> minimalConflictSet,
            AutoSchedulePlanDTO.PlanDiff planDiff,
            List<String> warnings,
            String planId) {
        Map<String, Double> score = result.scoreBreakdown();
        AutoSchedulePlanDTO.SoftScoreBreakdown breakdown = new AutoSchedulePlanDTO.SoftScoreBreakdown(
                score.getOrDefault("fairness", 0D),
                score.getOrDefault("preference", 0D),
                score.getOrDefault("continuity", 0D),
                score.getOrDefault("seniorCoverage", 0D),
                score.getOrDefault("weekendBalance", 0D)
        );
        AutoSchedulePlanDTO.ScoreSummary scoreSummary = new AutoSchedulePlanDTO.ScoreSummary(
                result.totalScore(),
                result.hardViolationCount(),
                breakdown
        );
        List<AutoSchedulePlanDTO.AssignmentExplanation> explanations = result.assignments().stream()
                .map(assignment -> new AutoSchedulePlanDTO.AssignmentExplanation(
                        assignment.date().toString(),
                        assignment.periodCode(),
                        assignment.doctorId(),
                        assignment.reasons(),
                        assignment.penalties()
                ))
                .toList();
        List<AutoSchedulePlanDTO.UnfilledSlot> unfilledSlots = result.unfilledSlots().stream()
                .map(slot -> new AutoSchedulePlanDTO.UnfilledSlot(
                        slot.date().toString(),
                        slot.periodCode(),
                        slot.missingDoctors(),
                        slot.reason()
                ))
                .toList();
        return new AutoSchedulePlanDTO(
                planId,
                generatedScheduleIds,
                scoreSummary,
                explanations,
                unfilledSlots,
                minimalConflictSet,
                planDiff,
                warnings
        );
    }

    private String buildPlanCode(Long departmentId, LocalDate startDate, LocalDate endDate) {
        return "AUTO-%d-%s-%s".formatted(departmentId, startDate, endDate);
    }

    private List<SchedulePlanItem> buildPlanItems(
            DepartmentScheduleOptimizationResult optimizationResult,
            List<ScheduleDoctorProfile> doctors) {
        return buildPlanItemsFromAssignments(optimizationResult.assignments(), doctors);
    }

    private List<SchedulePlanItem> buildPlanItemsFromAssignments(
            List<me.jianwen.mediask.schedule.domain.optimization.model.OptimizationAssignment> assignments,
            List<ScheduleDoctorProfile> doctors) {
        Set<Long> seniorDoctors = new HashSet<>(doctors.stream()
                .filter(ScheduleDoctorProfile::senior)
                .map(ScheduleDoctorProfile::doctorId)
                .toList());
        return assignments.stream()
                .map(assignment -> new SchedulePlanItem(
                        assignment.date(),
                        assignment.periodCode(),
                        assignment.doctorId(),
                        seniorDoctors.contains(assignment.doctorId()),
                        JsonUtil.toJson(assignment.reasons()),
                        JsonUtil.toJson(assignment.penalties()),
                        null
                ))
                .toList();
    }

    private List<SchedulePlanItem> mergeIncrementalPlanItems(
            List<SchedulePlanItem> basePlanItems,
            DepartmentScheduleOptimizationResult optimizationResult,
            List<ScheduleDoctorProfile> doctors,
            Set<Long> affectedDoctorIds,
            LocalDate planningStartDate,
            LocalDate planningEndDate) {
        List<SchedulePlanItem> optimizedItems = buildPlanItems(optimizationResult, doctors);
        Map<ItemKey, SchedulePlanItem> merged = new LinkedHashMap<>();

        for (SchedulePlanItem item : basePlanItems) {
            boolean inWindow = !item.scheduleDate().isBefore(planningStartDate) && !item.scheduleDate().isAfter(planningEndDate);
            boolean affectedDoctor = affectedDoctorIds.contains(item.doctorId());
            if (inWindow && affectedDoctor) {
                continue;
            }
            merged.put(new ItemKey(item.scheduleDate(), item.periodCode(), item.doctorId()), item);
        }
        for (SchedulePlanItem item : optimizedItems) {
            merged.put(new ItemKey(item.scheduleDate(), item.periodCode(), item.doctorId()), item);
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(SchedulePlanItem::scheduleDate)
                        .thenComparing(SchedulePlanItem::periodCode)
                        .thenComparing(SchedulePlanItem::doctorId))
                .toList();
    }

    private Set<Long> resolveAffectedDoctorIds(AutoScheduleCommand request, List<ScheduleDoctorProfile> doctors) {
        if (request.getDoctorIds() != null && !request.getDoctorIds().isEmpty()) {
            return new HashSet<>(request.getDoctorIds());
        }
        return new HashSet<>(doctors.stream().map(ScheduleDoctorProfile::doctorId).toList());
    }

    private AutoSchedulePlanDTO.PlanDiff toPlanDiffDTO(SchedulePlanDiffCalculator.PlanDiffResult diffResult) {
        if (diffResult == null) {
            return null;
        }
        List<AutoSchedulePlanDTO.DiffSlot> details = diffResult.changedDetails().stream()
                .map(item -> new AutoSchedulePlanDTO.DiffSlot(
                        item.changeType(),
                        item.date().toString(),
                        item.periodCode(),
                        item.beforeDoctorIds(),
                        item.afterDoctorIds()
                ))
                .toList();
        return new AutoSchedulePlanDTO.PlanDiff(
                diffResult.addedAssignments(),
                diffResult.removedAssignments(),
                diffResult.changedSlots(),
                diffResult.unchangedAssignments(),
                details
        );
    }

    private SchedulePlan resolveBasePlan(AutoScheduleCommand request) {
        SchedulePlan basePlan = schedulePlanRepository.findById(request.getBasePlanId())
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "增量重排基线方案不存在"));
        if (!basePlan.departmentId().equals(request.getDepartmentId())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "basePlanId 与 departmentId 不匹配");
        }
        if (request.getReplanWindowStartDate().isBefore(basePlan.startDate())
                || request.getReplanWindowEndDate().isAfter(basePlan.endDate())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "增量重排窗口超出基线方案日期范围");
        }
        return basePlan;
    }

    private String resolveConstraintDslJson(AutoScheduleCommand request) {
        if (request.getConstraintDslJson() != null && !request.getConstraintDslJson().isBlank()) {
            return request.getConstraintDslJson();
        }
        if (request.getRuleProfileCode() == null || request.getRuleProfileCode().isBlank()) {
            return null;
        }
        ScheduleRuleProfile profile = scheduleRuleProfileRepository.findPublishedByCode(
                        request.getDepartmentId(),
                        request.getRuleProfileCode().trim().toUpperCase(Locale.ROOT)
                )
                .orElseThrow(() -> new BizException(ErrorCode.DATA_NOT_FOUND, "未找到已发布规则配置: " + request.getRuleProfileCode()));
        return profile.constraintDslJson();
    }

    private record ItemKey(LocalDate date, Integer periodCode, Long doctorId) {
    }

    private List<SchedulePlanConstraintSnapshot> buildSnapshots(
            AutoScheduleCommand request,
            SchedulingEngineResult engineResult,
            String resolvedConstraintDslJson,
            SchedulePlan basePlan,
            List<DoctorAvailabilityRule> availabilityRules,
            List<DoctorTimeOff> timeOffRules,
            List<DepartmentScheduleDemand> demands,
            Set<LocalDate> holidayDates,
            Set<LocalDate> makeupWorkdayDates) {
        List<SchedulePlanConstraintSnapshot> snapshots = new ArrayList<>();
        snapshots.add(new SchedulePlanConstraintSnapshot("DOCTOR_RULES", JsonUtil.toJson(availabilityRules)));
        snapshots.add(new SchedulePlanConstraintSnapshot("TIME_OFF", JsonUtil.toJson(timeOffRules)));
        snapshots.add(new SchedulePlanConstraintSnapshot("DEMAND", JsonUtil.toJson(demands)));
        snapshots.add(new SchedulePlanConstraintSnapshot("CALENDAR", JsonUtil.toJson(Map.of(
                "holidayDates", holidayDates,
                "makeupWorkdayDates", makeupWorkdayDates
        ))));
        snapshots.add(new SchedulePlanConstraintSnapshot("HARD_SOFT", JsonUtil.toJson(Map.of(
                "hardConstraints", request.resolvedHardConstraints(),
                "softGoals", request.resolvedSoftGoals(),
                "solverConfig", request.resolvedSolverConfig()
        ))));
        Map<String, Object> dslSnapshot = new java.util.HashMap<>();
        dslSnapshot.put("constraintDslJson", resolvedConstraintDslJson);
        dslSnapshot.put("ruleProfileCode", request.getRuleProfileCode());
        snapshots.add(new SchedulePlanConstraintSnapshot("DSL_SOURCE", JsonUtil.toJson(dslSnapshot)));
        Map<String, Object> runMetaSnapshot = new java.util.HashMap<>();
        runMetaSnapshot.put("requestedStrategy", engineResult.requestedStrategy());
        runMetaSnapshot.put("actualStrategy", engineResult.actualStrategy());
        runMetaSnapshot.put("seed", engineResult.seed());
        runMetaSnapshot.put("minimalConflictSet", engineResult.minimalConflictSet());
        runMetaSnapshot.put("warnings", engineResult.warnings());
        snapshots.add(new SchedulePlanConstraintSnapshot("SOLVER_RUN_META", JsonUtil.toJson(runMetaSnapshot)));
        if (request.incrementalReplan()) {
            Map<String, Object> replanSnapshot = new HashMap<>();
            replanSnapshot.put("basePlanId", request.getBasePlanId());
            replanSnapshot.put("basePlanCode", basePlan == null ? null : basePlan.planCode());
            replanSnapshot.put("windowStartDate", request.getReplanWindowStartDate());
            replanSnapshot.put("windowEndDate", request.getReplanWindowEndDate());
            replanSnapshot.put("affectedDoctorIds", request.getDoctorIds());
            snapshots.add(new SchedulePlanConstraintSnapshot("INCREMENTAL_REPLAN", JsonUtil.toJson(replanSnapshot)));
        }
        return snapshots;
    }

    private ScheduleDTO toScheduleDTO(DoctorSchedule schedule) {
        return ScheduleDTO.builder()
                .scheduleId(schedule.getId() != null ? schedule.getId().getValue() : null)
                .doctorId(schedule.getDoctorId() != null ? schedule.getDoctorId().getValue() : null)
                .scheduleDate(schedule.getScheduleDate())
                .timePeriodCode(schedule.getTimePeriod() != null ? schedule.getTimePeriod().getCode() : null)
                .timePeriodDesc(schedule.getTimePeriod() != null ? schedule.getTimePeriod().getDescription() : null)
                .totalSlots(schedule.getCapacity() != null ? schedule.getCapacity().getTotalSlots() : null)
                .availableSlots(schedule.getCapacity() != null ? schedule.getCapacity().getAvailableSlots() : null)
                .statusCode(schedule.getStatus() != null ? schedule.getStatus().getCode() : null)
                .statusDesc(schedule.getStatus() != null ? schedule.getStatus().getDescription() : null)
                .slotDurationMinutes(schedule.getSlotDurationMinutes())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
