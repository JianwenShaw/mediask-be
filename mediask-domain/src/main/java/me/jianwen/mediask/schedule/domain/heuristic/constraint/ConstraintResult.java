package me.jianwen.mediask.schedule.domain.heuristic.constraint;

import java.util.Map;

/**
 * 约束检查结果
 *
 * <p>记录约束检查的详细信息：
 * <ul>
 *   <li>是否满足</li>
 *   <li>惩罚分数</li>
 *   <li>详细信息</li>
 * </ul>
 *
 * @param satisfied 是否满足约束
 * @param penalty   惩罚分数（0表示满足，越高表示违反越严重）
 * @param message   检查消息
 * @param details   详细信息（可用于调试和分析）
 */
public record ConstraintResult(
        boolean satisfied,
        double penalty,
        String message,
        Map<String, Object> details
) {

    /**
     * 创建一个成功的约束检查结果
     */
    public static ConstraintResult ok() {
        return new ConstraintResult(true, 0, "OK", Map.of());
    }

    /**
     * 创建一个成功的约束检查结果（带消息）
     */
    public static ConstraintResult ok(String message) {
        return new ConstraintResult(true, 0, message, Map.of());
    }

    /**
     * 创建一个成功的约束检查结果（带消息和详细信息）
     */
    public static ConstraintResult ok(String message, Map<String, Object> details) {
        return new ConstraintResult(true, 0, message, details);
    }

    /**
     * 创建一个成功的约束检查结果（带消息、惩罚分数和详细信息）
     */
    public static ConstraintResult ok(String message, double penalty, Map<String, Object> details) {
        return new ConstraintResult(true, penalty, message, details);
    }

    /**
     * 创建一个失败的约束检查结果
     */
    public static ConstraintResult fail(String message) {
        return new ConstraintResult(false, 1.0, message, Map.of());
    }

    /**
     * 创建一个失败的约束检查结果（带惩罚分数）
     */
    public static ConstraintResult fail(String message, double penalty) {
        return new ConstraintResult(false, penalty, message, Map.of());
    }

    /**
     * 创建一个失败的约束检查结果（带详细信息）
     */
    public static ConstraintResult fail(String message, Map<String, Object> details) {
        return new ConstraintResult(false, 1.0, message, details);
    }

    /**
     * 创建一个失败的约束检查结果（带惩罚分数和详细信息）
     */
    public static ConstraintResult fail(String message, double penalty, Map<String, Object> details) {
        return new ConstraintResult(false, penalty, message, details);
    }

    /**
     * 合并两个约束结果
     */
    public ConstraintResult merge(ConstraintResult other) {
        boolean satisfied = this.satisfied && other.satisfied;
        double penalty = this.penalty + other.penalty;
        String message = this.satisfied ? other.message : this.message;
        return new ConstraintResult(satisfied, penalty, message, this.details);
    }
}
