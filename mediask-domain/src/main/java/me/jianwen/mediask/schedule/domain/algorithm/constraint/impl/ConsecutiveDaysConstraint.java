package me.jianwen.mediask.schedule.domain.algorithm.constraint.impl;

import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 连续工作天数约束
 *
 * <p>硬约束 - 确保医生连续工作天数不超过上限</p>
 * <p>默认最大连续工作天数为5天</p>
 *
 * @author MediAsk
 */
@Slf4j
public class ConsecutiveDaysConstraint implements ScheduleConstraint {

    private static final int DEFAULT_MAX_CONSECUTIVE_DAYS = 5;

    @Override
    public String name() {
        return "ConsecutiveDaysConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.HARD;
    }

    @Override
    public ConstraintResult check(ScheduleContext context) {
        int maxDays = context.getProblem().getMaxConsecutiveDays();
        Map<String, Object> details = new HashMap<>();
        Map<Long, Integer> violations = new HashMap<>();

        for (Map.Entry<Long, Integer> entry : context.getConsecutiveDays().entrySet()) {
            Long doctorId = entry.getKey();
            int consecutive = entry.getValue();

            if (consecutive > maxDays) {
                violations.put(doctorId, consecutive);
                log.debug("医生 {} 连续工作 {} 天，超过上限 {}", doctorId, consecutive, maxDays);
            }
        }

        details.put("maxDays", maxDays);
        details.put("violations", violations);
        details.put("violationCount", violations.size());

        if (!violations.isEmpty()) {
            return ConstraintResult.fail(
                    String.format("发现 %d 位医生连续工作天数超标", violations.size()),
                    1.0,
                    details
            );
        }

        return ConstraintResult.ok("所有医生连续工作天数符合要求", details);
    }

    @Override
    public int priority() {
        return 90;
    }
}
