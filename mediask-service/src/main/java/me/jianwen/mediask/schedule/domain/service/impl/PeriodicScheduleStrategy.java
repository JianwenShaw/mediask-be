package me.jianwen.mediask.schedule.domain.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.rule.ScheduleRule;
import me.jianwen.mediask.schedule.domain.service.AutoScheduleStrategy;
import me.jianwen.mediask.schedule.domain.service.ScheduleContext;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 周期性排班策略
 * 根据规则在指定日期范围内循环生成排班
 * 
 * 适用场景：
 * - 医生固定工作日（如周一到周五）
 * - 每周固定时段出诊
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PeriodicScheduleStrategy implements AutoScheduleStrategy {

    @Override
    public String getStrategyName() {
        return "PERIODIC";
    }

    @Override
    public List<DoctorSchedule> generateSchedules(
            DoctorId doctorId,
            LocalDate startDate,
            LocalDate endDate,
            ScheduleContext context) {

        log.info("使用周期性排班策略为医生 {} 生成 {} 到 {} 的排班",
                doctorId.getValue(), startDate, endDate);

        ScheduleRule rule = context.getScheduleRule();
        List<DoctorSchedule> schedules = new ArrayList<>();

        // 遍历日期范围
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {

            // 检查规则是否在当前日期生效
            if (rule.isEffectiveOn(currentDate)) {

                // 获取当天应该排班的时段
                Set<TimePeriod> periods = rule.getEffectivePeriodsOn(currentDate);

                // 为每个时段生成排班
                for (TimePeriod period : periods) {
                    DoctorSchedule schedule = DoctorSchedule.create(
                            doctorId,
                            currentDate,
                            period,
                            rule.getSlotsPerPeriod(),
                            rule.getSlotDurationMinutes());
                    schedules.add(schedule);

                    log.debug("生成排班: 日期={}, 时段={}, 号源数={}",
                            currentDate, period.getDescription(), rule.getSlotsPerPeriod());
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("周期性排班策略完成，共生成 {} 条排班", schedules.size());
        return schedules;
    }

    @Override
    public boolean isApplicable(ScheduleContext context) {
        ScheduleRule rule = context.getScheduleRule();
        return rule != null
                && rule.getEffectiveDaysOfWeek() != null
                && !rule.getEffectiveDaysOfWeek().isEmpty();
    }
}
