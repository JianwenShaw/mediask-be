package me.jianwen.mediask.schedule.domain.algorithm.result;

import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.algorithm.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleContext;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 方案评估器
 *
 * <p>对排班方案进行全面评估：
 * <ul>
 *   <li>硬约束检查</li>
 *   <li>软约束评分</li>
 *   <li>综合评分计算</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class SolutionEvaluator {

    /**
     * 约束列表
     */
    private List<ScheduleConstraint> constraints;

    /**
     * 硬约束权重
     */
    @Builder.Default
    private double hardConstraintWeight = 50.0;

    /**
     * 软约束权重
     */
    @Builder.Default
    private double softConstraintWeight = 50.0;

    /**
     * 评估方案
     */
    public EvaluationResult evaluate(ScheduleContext context) {
        List<ConstraintResult> results = new ArrayList<>();
        double totalPenalty = 0;
        double totalWeight = 0;
        boolean allHardSatisfied = true;

        // 检查所有约束
        for (ScheduleConstraint constraint : constraints) {
            ConstraintResult result = constraint.check(context);
            results.add(result);

            if (constraint.type() == ConstraintType.HARD) {
                if (!result.satisfied()) {
                    allHardSatisfied = false;
                    totalPenalty += constraint.weight();
                }
            } else {
                // 软约束
                totalPenalty += result.penalty() * constraint.weight();
                totalWeight += constraint.weight();
            }
        }

        // 计算评分
        double hardScore = allHardSatisfied ? hardConstraintWeight : 0;
        double softScore = totalWeight > 0
                ? softConstraintWeight * (1 - totalPenalty / totalWeight)
                : softConstraintWeight;
        double totalScore = hardScore + Math.max(0, softScore);

        return EvaluationResult.builder()
                .constraintResults(results)
                .hardConstraintsSatisfied(allHardSatisfied)
                .hardScore(hardScore)
                .softScore(softScore)
                .totalScore(totalScore)
                .violationCount(results.stream().filter(r -> !r.satisfied()).count())
                .build();
    }

    /**
     * 评估结果
     */
    @Data
    @Builder
    public static class EvaluationResult {
        private List<ConstraintResult> constraintResults;
        private boolean hardConstraintsSatisfied;
        private double hardScore;
        private double softScore;
        private double totalScore;
        private long violationCount;

        /**
         * 判断方案是否可用
         */
        public boolean isAcceptable() {
            return hardConstraintsSatisfied && totalScore >= 60;
        }

        /**
         * 获取评分等级
         */
        public String getGrade() {
            if (totalScore >= 90) return "A";
            if (totalScore >= 80) return "B";
            if (totalScore >= 70) return "C";
            if (totalScore >= 60) return "D";
            return "F";
        }
    }

    /**
     * 创建默认评估器（包含所有约束）
     */
    public static SolutionEvaluator defaultEvaluator() {
        return SolutionEvaluator.builder()
                .constraints(List.of(
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.DoctorAvailabilityConstraint(),
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.ConsecutiveDaysConstraint(),
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.DailyAssignmentLimitConstraint(),
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.HolidayExclusionConstraint(),
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.WorkloadBalanceConstraint(),
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.TimePreferenceConstraint(),
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.ExpertCoverageConstraint(),
                        new me.jianwen.mediask.schedule.domain.algorithm.constraint.impl.GapMinimizationConstraint()
                ))
                .hardConstraintWeight(50.0)
                .softConstraintWeight(50.0)
                .build();
    }
}
