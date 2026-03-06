package me.jianwen.mediask.schedule.domain.heuristic.constraint.impl;

import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 节假日排除约束
 *
 * <p>硬约束 - 确保排班不安排在法定节假日</p>
 * <p>使用节假日服务判断日期是否为节假日</p>
 *
 * @author MediAsk
 */
@Slf4j
public class HolidayExclusionConstraint implements ScheduleConstraint {

    // 固定节假日（简化实现，实际应使用HolidayService）
    private static final Set<LocalDate> KNOWN_HOLIDAYS = Set.of(
            // 元旦
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2026, 1, 1),
            // 春节（简化示例）
            LocalDate.of(2025, 1, 29),
            LocalDate.of(2025, 1, 30),
            LocalDate.of(2025, 1, 31),
            LocalDate.of(2025, 2, 1),
            LocalDate.of(2025, 2, 2),
            LocalDate.of(2025, 2, 3),
            LocalDate.of(2025, 2, 4),
            // 清明节
            LocalDate.of(2025, 4, 4),
            LocalDate.of(2025, 4, 5),
            LocalDate.of(2025, 4, 6),
            // 劳动节
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 2),
            LocalDate.of(2025, 5, 3),
            LocalDate.of(2025, 5, 4),
            // 端午节
            LocalDate.of(2025, 5, 31),
            // 中秋节
            LocalDate.of(2025, 9, 21),
            // 国庆节
            LocalDate.of(2025, 10, 1),
            LocalDate.of(2025, 10, 2),
            LocalDate.of(2025, 10, 3),
            LocalDate.of(2025, 10, 4),
            LocalDate.of(2025, 10, 5),
            LocalDate.of(2025, 10, 6),
            LocalDate.of(2025, 10, 7)
    );

    @Override
    public String name() {
        return "HolidayExclusionConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.HARD;
    }

    @Override
    public ConstraintResult check(SolverContext context) {
        if (!context.getProblem().isExcludeHolidays()) {
            return ConstraintResult.ok("节假日排除已禁用");
        }

        Map<String, Object> details = new HashMap<>();
        Set<LocalDate> holidayAssignments = new HashSet<>();

        for (String key : context.getAssignments().keySet()) {
            String[] parts = key.split("_");
            if (parts.length < 2) {
                continue;
            }
            LocalDate date = LocalDate.parse(parts[0]);

            if (isHoliday(date)) {
                holidayAssignments.add(date);
                log.debug("排班安排在节假日 {}", date);
            }
        }

        details.put("holidayAssignments", holidayAssignments);
        details.put("violationCount", holidayAssignments.size());

        if (!holidayAssignments.isEmpty()) {
            return ConstraintResult.fail(
                    String.format("发现 %d 个排班安排在节假日", holidayAssignments.size()),
                    1.0,
                    details
            );
        }

        return ConstraintResult.ok("排班未安排在节假日", details);
    }

    /**
     * 检查日期是否为节假日
     */
    public boolean isHoliday(LocalDate date) {
        return KNOWN_HOLIDAYS.contains(date);
    }

    @Override
    public int priority() {
        return 70;
    }
}
