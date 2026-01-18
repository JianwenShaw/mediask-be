package me.jianwen.mediask.schedule.domain.algorithm.solver.history;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果
 *
 * <p>记录完整的搜索过程信息：
 * <ul>
 *   <li>搜索历史轨迹</li>
 *   <li>最优解信息</li>
 *   <li>统计信息</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class SearchResult {

    /**
     * 求解器名称
     */
    private String solverName;

    /**
     * 搜索开始时间
     */
    private LocalDateTime startTime;

    /**
     * 搜索结束时间
     */
    private LocalDateTime endTime;

    /**
     * 搜索耗时（毫秒）
     */
    private long durationMs;

    /**
     * 总迭代次数
     */
    private int totalIterations;

    /**
     * 改进次数
     */
    private int improvementCount;

    /**
     * 拒绝次数
     */
    private int rejectionCount;

    /**
     * 搜索步骤历史
     */
    @Builder.Default
    private List<SearchStep> steps = new ArrayList<>();

    /**
     * 最佳评分
     */
    private double bestScore;

    /**
     * 初始评分
     */
    private double initialScore;

    /**
     * 评分提升
     */
    private double scoreImprovement;

    /**
     * 最终是否收敛
     */
    private boolean converged;

    /**
     * 收敛原因
     */
    private String convergenceReason;

    /**
     * 添加搜索步骤
     */
    public void addStep(SearchStep step) {
        this.steps.add(step);
        this.totalIterations++;

        if (step.isSuccess()) {
            if (step.getOperationType() == SearchStep.OperationType.IMPROVEMENT
                    || step.getOperationType() == SearchStep.OperationType.SWAP) {
                this.improvementCount++;
            }
        } else {
            this.rejectionCount++;
        }
    }

    /**
     * 记录改进
     */
    public void recordImprovement(int step, String description, double before, double after) {
        SearchStep searchStep = SearchStep.improvement(step, description, before, after, null);
        addStep(searchStep);

        if (after > bestScore) {
            this.bestScore = after;
            this.scoreImprovement = after - initialScore;
        }
    }

    /**
     * 标记收敛
     */
    public void markConverged(String reason) {
        this.converged = true;
        this.convergenceReason = reason;
    }

    /**
     * 获取改进率
     */
    public double getImprovementRate() {
        if (initialScore <= 0) return 0;
        return (double) improvementCount / totalIterations * 100;
    }

    /**
     * 获取接受率
     */
    public double getAcceptanceRate() {
        if (totalIterations == 0) return 0;
        return (double) (totalIterations - rejectionCount) / totalIterations * 100;
    }

    /**
     * 获取摘要
     */
    public String getSummary() {
        return String.format(
                "搜索完成: 迭代%d次, 改进%d次, 接受率%.1f%%, 评分%.2f->%.2f (%+.2f), %s",
                totalIterations,
                improvementCount,
                getAcceptanceRate(),
                initialScore,
                bestScore,
                scoreImprovement,
                converged ? "已收敛" : "未收敛"
        );
    }

    /**
     * 获取前N个改进步骤
     */
    public List<SearchStep> getTopImprovements(int n) {
        return steps.stream()
                .filter(s -> s.getOperationType() == SearchStep.OperationType.IMPROVEMENT
                        || s.getOperationType() == SearchStep.OperationType.SWAP)
                .limit(n)
                .toList();
    }
}
