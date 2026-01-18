package me.jianwen.mediask.schedule.domain.algorithm.constraint.impl;

import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.algorithm.problem.Doctor;
import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 医生可用性约束
 *
 * <p>硬约束 - 确保医生在被排班时是可用的</p>
 * <p>检查医生是否在指定日期请假、进修或其他原因不可用</p>
 *
 * @author MediAsk
 */
@Slf4j
public class DoctorAvailabilityConstraint implements ScheduleConstraint {

    @Override
    public String name() {
        return "DoctorAvailabilityConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.HARD;
    }

    @Override
    public ConstraintResult check(ScheduleContext context) {
        Map<String, Object> details = new HashMap<>();
        int violationCount = 0;

        for (Map.Entry<String, Long> entry : context.getAssignments().entrySet()) {
            String key = entry.getKey();
            Long doctorId = entry.getValue();

            // 解析日期和时段
            String[] parts = key.split("_");
            if (parts.length < 2) {
                continue;
            }
            LocalDate date = LocalDate.parse(parts[0]);
            String period = parts[1];

            // 获取医生信息
            Doctor doctor = context.getProblem().getDoctorSet().getById(doctorId).orElse(null);
            if (doctor == null) {
                violationCount++;
                continue;
            }

            // 检查可用性
            if (!doctor.isAvailableOn(date)) {
                violationCount++;
                log.debug("医生 {} 在 {} 不可用", doctorId, date);
            }

            // 检查时段可用性
            if (!doctor.isAvailableInPeriod(period)) {
                violationCount++;
                log.debug("医生 {} 在时段 {} 不可用", doctorId, period);
            }
        }

        details.put("totalAssignments", context.getAssignments().size());
        details.put("violationCount", violationCount);

        if (violationCount > 0) {
            return ConstraintResult.fail(
                    String.format("发现 %d 个医生可用性违规", violationCount),
                    1.0,
                    details
            );
        }

        return ConstraintResult.ok("所有医生在排班日期均可用", details);
    }

    @Override
    public int priority() {
        return 100; // 最高优先级
    }
}
