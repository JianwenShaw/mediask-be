package me.jianwen.mediask.schedule.domain.algorithm.constraint.impl;

import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作量均衡约束
 *
 * <p>软约束 - 尽量使医生的工作量均衡</p>
 * <p>计算所有医生工作量的标准差，偏离均值越小评分越高</p>
 *
 * @author MediAsk
 */
@Slf4j
public class WorkloadBalanceConstraint implements ScheduleConstraint {

    private static final double DEFAULT_BALANCE_THRESHOLD = 2.0;

    @Override
    public String name() {
        return "WorkloadBalanceConstraint";
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.SOFT;
    }

    @Override
    public ConstraintResult check(ScheduleContext context) {
        if (!context.getProblem().isEnableWorkloadBalance()) {
            return ConstraintResult.ok("工作量均衡已禁用");
        }

        Map<Long, Integer> workloads = context.getTotalWorkload();
        if (workloads.isEmpty()) {
            return ConstraintResult.ok("无排班记录");
        }

        // 计算统计数据
        double mean = workloads.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        double variance = workloads.values().stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        double stdDev = Math.sqrt(variance);
        double threshold = context.getProblem().getWorkloadBalanceThreshold();

        // 计算均衡得分 (0-1, 越接近0越均衡)
        double balanceScore = Math.min(stdDev / threshold, 1.0);
        double satisfaction = 1.0 - balanceScore;

        Map<String, Object> details = new HashMap<>();
        details.put("mean", mean);
        details.put("stdDev", stdDev);
        details.put("threshold", threshold);
        details.put("balanceScore", balanceScore);

        // 按工作量排序
        Map<Long, Integer> sortedWorkloads = workloads.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        details.put("workloads", sortedWorkloads);

        if (stdDev <= threshold) {
            return ConstraintResult.ok(
                    String.format("工作量均衡达标，标准差=%.2f", stdDev),
                    satisfaction,
                    details
            );
        }

        // 返回惩罚分数
        double penalty = Math.min(balanceScore, 1.0);
        return ConstraintResult.fail(
                String.format("工作量不均衡，标准差=%.2f", stdDev),
                penalty,
                details
        );
    }

    @Override
    public double weight() {
        return 1.5; // 较高权重
    }

    @Override
    public int priority() {
        return 50;
    }
}
