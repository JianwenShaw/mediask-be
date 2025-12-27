package me.jianwen.mediask.schedule.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.application.command.AutoScheduleCommand;
import me.jianwen.mediask.schedule.application.command.CreateScheduleCommand;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.repository.DoctorScheduleRepository;
import me.jianwen.mediask.schedule.domain.rule.ScheduleRule;
import me.jianwen.mediask.schedule.domain.service.AutoScheduleService;
import me.jianwen.mediask.schedule.domain.service.ScheduleContext;
import me.jianwen.mediask.schedule.domain.service.SlotManagementService;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleStatus;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
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
 * 4. DTO/Command 到领域对象的转换
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleApplicationService {

    private final DoctorScheduleRepository scheduleRepository;
    private final AutoScheduleService autoScheduleService;
    private final SlotManagementService slotManagementService;

    /**
     * 创建单个排班
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(CreateScheduleCommand command) {
        log.info("创建排班: doctorId={}, date={}, period={}",
                command.getDoctorId(), command.getScheduleDate(), command.getTimePeriodCode());

        DoctorId doctorId = DoctorId.of(command.getDoctorId());
        TimePeriod timePeriod = command.getTimePeriod();

        // 1. 检查是否已存在排班
        if (scheduleRepository.exists(doctorId, command.getScheduleDate(), timePeriod)) {
            throw new IllegalArgumentException("该时段的排班已存在");
        }

        // 2. 创建排班聚合
        DoctorSchedule schedule = DoctorSchedule.create(
                doctorId,
                command.getScheduleDate(),
                timePeriod,
                command.getTotalSlots(),
                command.getSlotDurationMinutes());

        // 3. 保存排班
        scheduleRepository.save(schedule);

        // 4. 生成号源时段
        List<AppointmentSlot> slots = slotManagementService.generateSlotsForSchedule(schedule);
        slotManagementService.saveSlots(slots);

        // 5. 发布领域事件（在实际项目中需要实现事件发布器）
        publishEvents(schedule);

        log.info("排班创建成功: scheduleId={}", schedule.getId());

        return schedule.getId().getValue();
    }

    /**
     * 执行自动排班
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> autoSchedule(AutoScheduleCommand command) {
        log.info("执行自动排班: doctorId={}, dateRange={} to {}",
                command.getDoctorId(), command.getStartDate(), command.getEndDate());

        // 1. 构建排班规则
        ScheduleRule rule = new ScheduleRule();
        rule.setRuleName("自动排班规则");
        rule.setEffectiveDaysOfWeek(command.getWorkDays());
        rule.setEffectivePeriods(command.getTimePeriods());
        rule.setSlotsPerPeriod(command.getSlotsPerPeriod());
        rule.setSlotDurationMinutes(command.getSlotDurationMinutes());
        rule.setEffectiveStartDate(command.getStartDate());
        rule.setEffectiveEndDate(command.getEndDate());
        rule.setExcludeHolidays(command.getExcludeHolidays());

        // 2. 构建排班上下文
        ScheduleContext context = ScheduleContext.builder()
                .scheduleRule(rule)
                .properties(new HashMap<>())
                .build();

        // 3. 执行自动排班
        DoctorId doctorId = DoctorId.of(command.getDoctorId());
        List<DoctorSchedule> schedules = autoScheduleService.autoSchedule(
                doctorId,
                command.getStartDate(),
                command.getEndDate(),
                context,
                command.getStrategyName());

        // 4. 保存排班
        autoScheduleService.saveSchedules(schedules);

        // 5. 为每个排班生成号源时段
        schedules.forEach(schedule -> {
            List<AppointmentSlot> slots = slotManagementService.generateSlotsForSchedule(schedule);
            slotManagementService.saveSlots(slots);
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

        DoctorSchedule schedule = getScheduleById(scheduleId);
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

        DoctorSchedule schedule = getScheduleById(scheduleId);
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

        DoctorSchedule schedule = getScheduleById(scheduleId);
        schedule.adjustTotalSlots(newTotalSlots);
        scheduleRepository.save(schedule);

        // TODO: 同步调整时段数量

        publishEvents(schedule);

        log.info("号源调整成功: scheduleId={}", scheduleId);
    }

    /**
     * 查询排班详情
     */
    public DoctorSchedule getScheduleById(Long scheduleId) {
        return scheduleRepository.findById(ScheduleId.of(scheduleId))
                .orElseThrow(() -> new IllegalArgumentException("排班不存在: " + scheduleId));
    }

    /**
     * 查询医生在日期范围内的排班
     */
    public List<DoctorSchedule> listSchedulesByDoctorAndDateRange(
            Long doctorId, LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByDoctorAndDateRange(
                DoctorId.of(doctorId),
                startDate,
                endDate);
    }

    /**
     * 查询可预约的排班
     */
    public List<DoctorSchedule> listOpenSchedules(LocalDate date, TimePeriod period) {
        return scheduleRepository.findOpenSchedulesByDateAndPeriod(date, period);
    }

    /**
     * 标记过期排班
     * 定时任务调用，将已过期的排班标记为已过期状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void markExpiredSchedules() {
        log.info("开始标记过期排班");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<DoctorSchedule> expiredSchedules = scheduleRepository.findExpiredSchedules(yesterday);

        expiredSchedules.forEach(schedule -> {
            if (schedule.getStatus() != ScheduleStatus.EXPIRED) {
                schedule.markAsExpired();
                scheduleRepository.save(schedule);
            }
        });

        log.info("过期排班标记完成: 共标记 {} 条", expiredSchedules.size());
    }

    /**
     * 发布领域事件
     */
    private void publishEvents(DoctorSchedule schedule) {
        // TODO: 实现事件发布器
        // eventPublisher.publishAll(schedule.getDomainEvents());
        schedule.clearDomainEvents();
    }
}
