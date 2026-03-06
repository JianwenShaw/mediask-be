package me.jianwen.mediask.schedule.domain.heuristic.constraint;

import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;

/**
 * 排班约束接口 - 核心抽象
 *
 * <p>所有排班约束都实现此接口，支持硬约束（HARD）和软约束（SOFT）：
 * <ul>
 *   <li>HARD约束：必须满足，否则方案无效</li>
 *   <li>SOFT约束：尽量满足，影响方案评分</li>
 * </ul>
 *
 * @author MediAsk
 */
public interface ScheduleConstraint {

    /**
     * 约束名称
     */
    String name();

    /**
     * 约束类型
     */
    ConstraintType type();

    /**
     * 检查约束是否满足
     *
     * @param context 排班上下文
     * @return 约束检查结果
     */
    ConstraintResult check(SolverContext context);

    /**
     * 约束权重（仅软约束使用）
     */
    default double weight() {
        return 1.0;
    }

    /**
     * 约束优先级（数值越大优先级越高）
     */
    default int priority() {
        return 0;
    }
}
