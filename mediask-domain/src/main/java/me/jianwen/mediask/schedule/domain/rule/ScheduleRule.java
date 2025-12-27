package me.jianwen.mediask.schedule.domain.rule;

import lombok.Data;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 排班规则值对象
 * 定义自动排班的规则
 *
 * @author jianwen
 */
@Data
public class ScheduleRule {

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 生效的星期几（1-7，周一到周日）
     */
    private Set<DayOfWeek> effectiveDaysOfWeek;

    /**
     * 生效的时段
     */
    private Set<TimePeriod> effectivePeriods;

    /**
     * 每个时段的号源数
     */
    private int slotsPerPeriod;

    /**
     * 每个号源的时长（分钟）
     */
    private int slotDurationMinutes;

    /**
     * 规则生效的日期范围
     */
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;

    /**
     * 是否排除法定节假日
     */
    private boolean excludeHolidays;

    /**
     * 检查规则是否在指定日期生效
     */
    public boolean isEffectiveOn(LocalDate date) {
        // 检查日期范围
        if (date.isBefore(effectiveStartDate) || date.isAfter(effectiveEndDate)) {
            return false;
        }

        // 检查星期几
        if (!effectiveDaysOfWeek.contains(date.getDayOfWeek())) {
            return false;
        }

        // TODO: 检查是否为法定节假日

        return true;
    }

    /**
     * 获取指定日期应该排班的时段
     */
    public Set<TimePeriod> getEffectivePeriodsOn(LocalDate date) {
        if (!isEffectiveOn(date)) {
            return Set.of();
        }
        return effectivePeriods;
    }
}
