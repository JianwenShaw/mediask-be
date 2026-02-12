package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.dto.schedule.ScheduleDTO;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.model.PageResult;
import me.jianwen.mediask.domain.event.DomainEventPublisher;
import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.AppointmentRepository;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.rule.ScheduleRule;
import me.jianwen.mediask.schedule.domain.service.AutoScheduleDomainService;
import me.jianwen.mediask.schedule.domain.service.ScheduleContext;
import me.jianwen.mediask.schedule.domain.service.SlotManagementDomainService;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import me.jianwen.mediask.service.application.command.AutoScheduleCommand;
import me.jianwen.mediask.service.application.command.CreateScheduleCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

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
    private final AutoScheduleDomainService autoScheduleDomainService;
    private final SlotManagementDomainService slotManagementDomainService;
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
    public List<Long> autoSchedule(AutoScheduleCommand request) {
        log.info("执行自动排班: doctorId={}, dateRange={} to {}",
                request.getDoctorId(), request.getStartDate(), request.getEndDate());

        // 1. 构建排班规则
        ScheduleRule rule = new ScheduleRule();
        rule.setRuleName("自动排班规则");
        rule.setEffectiveDaysOfWeek(request.getWorkDays());
        rule.setEffectivePeriods(request.getTimePeriods());
        rule.setSlotsPerPeriod(request.getSlotsPerPeriod());
        rule.setSlotDurationMinutes(request.getSlotDurationMinutes());
        rule.setEffectiveStartDate(request.getStartDate());
        rule.setEffectiveEndDate(request.getEndDate());
        rule.setExcludeHolidays(request.getExcludeHolidays());

        // 2. 构建排班上下文
        ScheduleContext context = ScheduleContext.builder()
                .scheduleRule(rule)
                .properties(new HashMap<>())
                .build();

        // 3. 执行自动排班
        DoctorId doctorId = DoctorId.of(request.getDoctorId());
        List<DoctorSchedule> schedules = autoScheduleDomainService.autoSchedule(
                doctorId,
                request.getStartDate(),
                request.getEndDate(),
                context,
                request.getStrategyName());

        // 4. 保存排班
        autoScheduleDomainService.saveSchedules(schedules);

        // 5. 为每个排班生成号源时段
        schedules.forEach(schedule -> {
            List<AppointmentSlot> slots = slotManagementDomainService.generateSlotsForSchedule(schedule);
            slotManagementDomainService.saveSlots(slots);
        });

        // 6. 发布领域事件
        schedules.forEach(this::publishEvents);

        log.info("自动排班完成: 生成 {} 条排班", schedules.size());

        return schedules.stream()
                .map(s -> s.getId().getValue())
                .toList();
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
