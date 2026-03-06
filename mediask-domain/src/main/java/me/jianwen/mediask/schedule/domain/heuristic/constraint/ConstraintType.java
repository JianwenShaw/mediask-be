package me.jianwen.mediask.schedule.domain.heuristic.constraint;

/**
 * 约束类型枚举
 *
 * <p>区分硬约束和软约束：
 * <ul>
 *   <li>HARD：必须满足的约束，违反则方案无效</li>
 *   <li>SOFT：尽量满足的约束，影响方案评分</li>
 * </ul>
 */
public enum ConstraintType {

    /**
     * 硬约束 - 必须满足
     * 违反硬约束的排班方案将被视为无效
     */
    HARD,

    /**
     * 软约束 - 尽量满足
     * 软约束用于优化排班质量，计算评分时考虑
     */
    SOFT
}
