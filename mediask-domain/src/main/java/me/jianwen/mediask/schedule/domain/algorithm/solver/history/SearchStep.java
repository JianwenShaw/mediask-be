package me.jianwen.mediask.schedule.domain.algorithm.solver.history;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * 搜索步骤
 *
 * <p>记录搜索过程中的每一步操作：
 * <ul>
 *   <li>步骤编号</li>
 *   <li>操作类型</li>
 *   <li>操作详情</li>
 *   <li>评分变化</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class SearchStep {

    /**
     * 步骤编号
     */
    private int stepNumber;

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 操作详情
     */
    private String description;

    /**
     * 操作前的评分
     */
    private double scoreBefore;

    /**
     * 操作后的评分
     */
    private double scoreAfter;

    /**
     * 评分变化
     */
    private double scoreDelta;

    /**
     * 改变的排班 (key: date_period, value: doctorId)
     */
    private Map<String, Long> changedAssignments;

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 额外信息
     */
    private Map<String, Object> metadata;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        INITIALIZE,    // 初始化
        ASSIGN,        // 分配
        SWAP,          // 交换
        MOVE,          // 移动
        UNDO,          // 撤销
        ACCEPT,        // 接受
        REJECT,        // 拒绝
        IMPROVEMENT,   // 改进
        CONVERGENCE    // 收敛
    }

    /**
     * 创建改进步骤
     */
    public static SearchStep improvement(
            int step,
            String description,
            double before,
            double after,
            Map<String, Long> changes) {

        return SearchStep.builder()
                .stepNumber(step)
                .operationType(OperationType.IMPROVEMENT)
                .description(description)
                .scoreBefore(before)
                .scoreAfter(after)
                .scoreDelta(after - before)
                .changedAssignments(changes)
                .success(true)
                .build();
    }

    /**
     * 创建交换步骤
     */
    public static SearchStep swap(
            int step,
            String date1,
            String period1,
            String date2,
            String period2,
            double before,
            double after) {

        Map<String, Long> changes = Map.of(
                date1 + "_" + period1, -1L,
                date2 + "_" + period2, -1L
        );

        return SearchStep.builder()
                .stepNumber(step)
                .operationType(OperationType.SWAP)
                .description(String.format("交换 %s-%s 与 %s-%s", date1, period1, date2, period2))
                .scoreBefore(before)
                .scoreAfter(after)
                .scoreDelta(after - before)
                .changedAssignments(changes)
                .success(true)
                .build();
    }

    /**
     * 创建拒绝步骤
     */
    public static SearchStep rejected(
            int step,
            String description,
            double score) {

        return SearchStep.builder()
                .stepNumber(step)
                .operationType(OperationType.REJECT)
                .description(description)
                .scoreBefore(score)
                .scoreAfter(score)
                .scoreDelta(0)
                .success(false)
                .build();
    }
}
