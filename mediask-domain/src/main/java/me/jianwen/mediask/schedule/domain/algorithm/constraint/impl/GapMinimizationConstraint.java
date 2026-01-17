package me.jianwen.mediask.schedule.domain.algorithm.constraint.impl;

import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.algorithm.problem.Doctor;
import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 空档最小化约束
 *
 * <p>软约束 - 尽量减少医生的空档期</p>
 * <p>空档期指医生连续工作后休息的天数，理想情况下应该均匀分布</p>
 *
 * @author MediAsk
 */
@Slf4j
public class GapMinimizationConstraint implements ScheduleConstraint {

    private static final int IDEAL_GAP_DAYS = 1; // 理想间隔天数

    @Override
    public String name() {
        return "GapMinimizationConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.SOFT;
    }

    @Override
    public ConstraintResult check(ScheduleContext context) {
        if (!context.getProblem().isEnableGapMinimization()) {
            return ConstraintResult.ok("空档最小化已禁用");
        }

        var problem = context.getProblem();
        var dateRange = problem.getDateRange();
        List<LocalDate> allDates = dateRange.getAllDates();

        Map<String, Object> details = new HashMap<>();
        Map<Long, List<LocalDate>> doctorAssignments = new HashMap<>();

        // 按医生分组排班日期
        for (Map.Entry<String, Long> entry : context.getAssignments().entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length < 2) {
                continue;
            }
            LocalDate date = LocalDate.parse(parts[0]);
            Long doctorId = entry.getValue();

            // 跳过周末
            if (dateRange.isWeekend(date)) {
                continue;
            }

            doctorAssignments
                    .computeIfAbsent(doctorId, k -> new ArrayList<>())
                    .add(date);
        }

        // 计算每个医生的空档期
        double totalGapScore = 0;
        int doctorCount = 0;
        Map<Long, Double> doctorGapScores = new HashMap<>();

        for (Map.Entry<Long, List<LocalDate>> entry : doctorAssignments.entrySet()) {
            Long doctorId = entry.getKey();
            List<LocalDate> assignedDates = entry.getValue();

            if (assignedDates.size() < 2) {
                continue;
            }

            // 排序日期
            assignedDates.sort(LocalDate::compareTo);

            // 计算相邻排班之间的间隔
            List<Integer> gaps = new ArrayList<>();
            for (int i = 1; i < assignedDates.size(); i++) {
                int gap = (int) java.time.temporal.ChronoUnit.DAYS
                        .between(assignedDates.get(i - 1), assignedDates.get(i));
                gaps.add(gap);
            }

            // 计算间隔的标准差（理想情况下间隔应该均匀）
            double meanGap = gaps.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);

            double variance = gaps.stream()
                    .mapToDouble(g -> Math.pow(g - meanGap, 2))
                    .average()
                    .orElse(0.0);
            double gapStdDev = Math.sqrt(variance);

            // 标准化评分（标准差越小越好）
            double gapScore = Math.min(gapStdDev / 3.0, 1.0);
            doctorGapScores.put(doctorId, gapScore);
            totalGapScore += gapScore;
            doctorCount++;
        }

        details.put("doctorGapScores", doctorGapScores);
        details.put("avgGapScore", doctorCount > 0 ? totalGapScore / doctorCount : 0);

        if (doctorCount == 0) {
            return ConstraintResult.ok("无足够排班记录评估空档");
        }

        double avgScore = totalGapScore / doctorCount;
        double satisfaction = 1.0 - avgScore;

        if (avgScore < 0.3) {
            return ConstraintResult.ok(
                    String.format("空档分布均匀，评分=%.2f", avgScore),
                    satisfaction,
                    details
            );
        }

        double penalty = avgScore;
        return ConstraintResult.fail(
                String.format("空档分布不均匀，评分=%.2f", avgScore),
                penalty,
                details
        );
    }

    @Override
    public double weight() {
        return 0.8;
    }

    @Override
    public int priority() {
        return 30;
    }
}
