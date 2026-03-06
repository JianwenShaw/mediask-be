package me.jianwen.mediask.schedule.domain.heuristic.problem;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 日期范围
 *
 * <p>封装排班的日期区间，提供日期遍历和节假日判断功能。
 *
 * @author MediAsk
 */
@Data
@Builder
public class DateRange {

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 创建日期范围
     */
    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        return new DateRange(startDate, endDate);
    }

    /**
     * 创建指定天数的日期范围（从明天开始）
     */
    public static DateRange ofDays(int days) {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(days - 1);
        return of(start, end);
    }

    /**
     * 获取日期范围内的所有日期
     */
    public List<LocalDate> getAllDates() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    /**
     * 获取日期范围天数
     */
    public long getTotalDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * 检查日期是否在范围内
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 获取周几（1=周一, 7=周日）
     */
    public int getDayOfWeek(LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        return dow; // 1-7 (周一到周日)
    }

    /**
     * 检查是否为周末
     */
    public boolean isWeekend(LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        return dow == 6 || dow == 7; // 周六或周日
    }
}
