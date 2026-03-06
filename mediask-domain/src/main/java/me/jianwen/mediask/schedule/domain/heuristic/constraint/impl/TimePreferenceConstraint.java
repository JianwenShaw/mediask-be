package me.jianwen.mediask.schedule.domain.heuristic.constraint.impl;

import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.heuristic.problem.Doctor;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 时段偏好约束
 *
 * <p>软约束 - 尽量满足医生的时段偏好</p>
 * <p>如果医生有偏好时段，优先安排在偏好时段</p>
 *
 * @author MediAsk
 */
@Slf4j
public class TimePreferenceConstraint implements ScheduleConstraint {

    @Override
    public String name() {
        return "TimePreferenceConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.SOFT;
    }

    @Override
    public ConstraintResult check(SolverContext context) {
        if (!context.getProblem().isEnableTimePreference()) {
            return ConstraintResult.ok("时段偏好已禁用");
        }

        int totalAssignments = 0;
        int preferenceMatched = 0;
        int preferenceMissed = 0;
        Map<String, Object> details = new HashMap<>();

        for (Map.Entry<String, Long> entry : context.getAssignments().entrySet()) {
            String key = entry.getKey();
            Long doctorId = entry.getValue();

            String[] parts = key.split("_");
            if (parts.length < 2) {
                continue;
            }
            LocalDate date = LocalDate.parse(parts[0]);
            String period = parts[1];

            // 跳过节假日和周末（已有其他约束处理）
            if (context.getProblem().getDateRange().isWeekend(date)) {
                continue;
            }

            Doctor doctor = context.getProblem().getDoctorSet()
                    .getById(doctorId)
                    .orElse(null);

            if (doctor == null) {
                continue;
            }

            totalAssignments++;

            // 检查是否匹配偏好
            if (doctor.prefersPeriod(period)) {
                preferenceMatched++;
            } else {
                preferenceMissed++;
                log.debug("医生 {} 的排班 {} 未匹配偏好时段 {}",
                        doctorId, key, period);
            }
        }

        details.put("totalAssignments", totalAssignments);
        details.put("preferenceMatched", preferenceMatched);
        details.put("preferenceMissed", preferenceMissed);

        if (totalAssignments == 0) {
            return ConstraintResult.ok("无排班记录");
        }

        double matchRate = (double) preferenceMatched / totalAssignments;
        details.put("matchRate", matchRate);

        // 偏好匹配率 >= 80% 为优秀
        if (matchRate >= 0.8) {
            return ConstraintResult.ok(
                    String.format("时段偏好匹配率=%.1f%%", matchRate * 100),
                    matchRate,
                    details
            );
        }

        // 返回惩罚分数
        double penalty = 1.0 - matchRate;
        return ConstraintResult.fail(
                String.format("时段偏好匹配率低=%.1f%%", matchRate * 100),
                penalty,
                details
        );
    }

    @Override
    public double weight() {
        return 1.0;
    }

    @Override
    public int priority() {
        return 40;
    }
}
