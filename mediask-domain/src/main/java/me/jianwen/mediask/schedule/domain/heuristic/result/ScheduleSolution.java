package me.jianwen.mediask.schedule.domain.heuristic.result;

import lombok.Builder;
import lombok.Data;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;
import me.jianwen.mediask.schedule.domain.heuristic.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.*;

/**
 * 排班解决方案
 *
 * <p>包含排班结果和评估信息：
 * <ul>
 *   <li>排班分配结果</li>
 *   <li>约束检查结果</li>
 *   <li>评分信息</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class ScheduleSolution {

    /**
     * 是否成功生成方案
     */
    private boolean success;

    /**
     * 使用的求解器名称
     */
    private String solverName;

    /**
     * 原始问题
     */
    private ScheduleProblem problem;

    /**
     * 排班分配结果
     */
    private SolverContext context;

    /**
     * 约束检查结果
     */
    @Builder.Default
    private List<ConstraintResult> constraintResults = new ArrayList<>();

    /**
     * 总体评分 (0-100)
     */
    private double score;

    /**
     * 硬约束是否全部满足
     */
    private boolean hardConstraintsSatisfied;

    /**
     * 软约束平均满足度
     */
    private double softConstraintsSatisfaction;

    /**
     * 生成时间（毫秒）
     */
    private long generationTimeMs;

    /**
     * 排班数量
     */
    private int assignmentCount;

    /**
     * 错误消息（如果失败）
     */
    private String errorMessage;

    /**
     * 创建成功方案
     */
    public static ScheduleSolution success(
            String solverName,
            ScheduleProblem problem,
            SolverContext context,
            List<ConstraintResult> constraintResults,
            double score,
            long generationTimeMs) {

        int assignmentCount = context.getAssignments().size();
        boolean hardSatisfied = constraintResults.stream()
                .allMatch(r -> r.satisfied());

        double softSatisfaction = constraintResults.stream()
                .filter(r -> !r.satisfied())
                .mapToDouble(r -> 1.0 - r.penalty())
                .reduce(1.0, (a, b) -> a * b);

        return ScheduleSolution.builder()
                .success(true)
                .solverName(solverName)
                .problem(problem)
                .context(context)
                .constraintResults(constraintResults)
                .score(score)
                .hardConstraintsSatisfied(hardSatisfied)
                .softConstraintsSatisfaction(softSatisfaction)
                .generationTimeMs(generationTimeMs)
                .assignmentCount(assignmentCount)
                .build();
    }

    /**
     * 创建失败方案
     */
    public static ScheduleSolution failure(
            String solverName,
            ScheduleProblem problem,
            String errorMessage) {
        return ScheduleSolution.builder()
                .success(false)
                .solverName(solverName)
                .problem(problem)
                .errorMessage(errorMessage)
                .score(0)
                .hardConstraintsSatisfied(false)
                .softConstraintsSatisfaction(0)
                .generationTimeMs(0)
                .assignmentCount(0)
                .build();
    }

    /**
     * 获取指定日期的排班医生
     */
    public Long getDoctorAt(LocalDate date, String period) {
        return context.getAssignedDoctor(date, period);
    }

    /**
     * 获取所有排班键
     */
    public Set<String> getAssignmentKeys() {
        return context.getAssignments().keySet();
    }

    /**
     * 获取解决方案摘要
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("success", success);
        summary.put("solverName", solverName);
        summary.put("score", score);
        summary.put("assignmentCount", assignmentCount);
        summary.put("hardConstraintsSatisfied", hardConstraintsSatisfied);
        summary.put("softConstraintsSatisfaction", softConstraintsSatisfaction);
        summary.put("generationTimeMs", generationTimeMs);
        return summary;
    }
}
