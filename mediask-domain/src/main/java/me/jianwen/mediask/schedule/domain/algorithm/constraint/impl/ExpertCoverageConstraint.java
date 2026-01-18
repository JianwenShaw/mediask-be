package me.jianwen.mediask.schedule.domain.algorithm.constraint.impl;

import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 专家号覆盖率约束
 *
 * <p>软约束 - 尽量保证专家号的覆盖率</p>
 * <p>确保每天每个时段都有专家坐诊（除非强制使用专家已禁用）</p>
 *
 * @author MediAsk
 */
@Slf4j
public class ExpertCoverageConstraint implements ScheduleConstraint {

    @Override
    public String name() {
        return "ExpertCoverageConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.SOFT;
    }

    @Override
    public ConstraintResult check(ScheduleContext context) {
        if (!context.getProblem().isRequireExpertCoverage()) {
            return ConstraintResult.ok("专家号覆盖要求已禁用");
        }

        var problem = context.getProblem();
        var dateRange = problem.getDateRange();
        var timeConfig = problem.getTimeConfig();
        Set<String> enabledPeriods = timeConfig.getEnabledPeriods();

        // 统计专家排班
        Map<String, Long> expertAssignments = new HashMap<>();
        int totalSlots = 0;
        int expertSlots = 0;

        for (String key : context.getAssignments().keySet()) {
            String[] parts = key.split("_");
            if (parts.length < 2) {
                continue;
            }
            LocalDate date = LocalDate.parse(parts[0]);
            String period = parts[1];

            // 跳过周末和节假日
            if (dateRange.isWeekend(date)) {
                continue;
            }

            Long doctorId = context.getAssignments().get(key);
            if (doctorId == null) {
                continue;
            }

            var doctorOpt = problem.getDoctorSet().getById(doctorId);
            if (doctorOpt.isEmpty()) {
                continue;
            }

            totalSlots++;
            if (doctorOpt.get().isExpert()) {
                expertSlots++;
            }
        }

        double coverageRate = totalSlots > 0
                ? (double) expertSlots / totalSlots
                : 0.0;
        double minRate = problem.getMinExpertCoverageRate();

        Map<String, Object> details = new HashMap<>();
        details.put("totalSlots", totalSlots);
        details.put("expertSlots", expertSlots);
        details.put("coverageRate", coverageRate);
        details.put("minRequiredRate", minRate);

        if (totalSlots == 0) {
            return ConstraintResult.ok("无排班记录");
        }

        // 计算满足度
        double satisfaction = Math.min(coverageRate / minRate, 1.0);

        if (coverageRate >= minRate) {
            return ConstraintResult.ok(
                    String.format("专家号覆盖率达标=%.1f%% (要求≥%.0f%%)",
                            coverageRate * 100, minRate * 100),
                    satisfaction,
                    details
            );
        }

        double penalty = 1.0 - satisfaction;
        return ConstraintResult.fail(
                String.format("专家号覆盖率不足=%.1f%% (要求≥%.0f%%)",
                        coverageRate * 100, minRate * 100),
                penalty,
                details
        );
    }

    @Override
    public double weight() {
        return 1.2;
    }

    @Override
    public int priority() {
        return 60;
    }
}
