package me.jianwen.mediask.schedule.domain.heuristic.constraint.impl;

import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 每日排班次数上限约束
 *
 * <p>硬约束 - 确保医生每日排班次数不超过上限</p>
 * <p>默认每日最大排班次数为3次</p>
 *
 * @author MediAsk
 */
@Slf4j
public class DailyAssignmentLimitConstraint implements ScheduleConstraint {

    private static final int DEFAULT_MAX_DAILY_ASSIGNMENTS = 3;

    @Override
    public String name() {
        return "DailyAssignmentLimitConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.HARD;
    }

    @Override
    public ConstraintResult check(SolverContext context) {
        int maxDaily = context.getProblem().getMaxDailyAssignments();
        Map<String, Object> details = new HashMap<>();
        Map<Long, Map<String, Integer>> violations = new HashMap<>();

        for (Map.Entry<Long, Map<java.time.LocalDate, Integer>> entry :
                context.getDailyAssignments().entrySet()) {

            Long doctorId = entry.getKey();
            Map<java.time.LocalDate, Integer> dailyCounts = entry.getValue();

            for (Map.Entry<java.time.LocalDate, Integer> dayEntry : dailyCounts.entrySet()) {
                java.time.LocalDate date = dayEntry.getKey();
                int count = dayEntry.getValue();

                if (count > maxDaily) {
                    violations
                            .computeIfAbsent(doctorId, k -> new HashMap<>())
                            .put(date.toString(), count);
                    log.debug("医生 {} 在 {} 有 {} 次排班，超过上限 {}",
                            doctorId, date, count, maxDaily);
                }
            }
        }

        details.put("maxDaily", maxDaily);
        details.put("violations", violations);
        details.put("violationCount", violations.size());

        if (!violations.isEmpty()) {
            return ConstraintResult.fail(
                    String.format("发现 %d 位医生每日排班次数超标", violations.size()),
                    1.0,
                    details
            );
        }

        return ConstraintResult.ok("所有医生每日排班次数符合要求", details);
    }

    @Override
    public int priority() {
        return 80;
    }
}
