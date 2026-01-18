package me.jianwen.mediask.schedule.domain.algorithm.solver;

import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleProblem;
import me.jianwen.mediask.schedule.domain.algorithm.result.ScheduleSolution;

/**
 * 排班求解器接口
 *
 * <p>定义了排班优化的求解算法接口，支持：
 * <ul>
 *   <li>贪婪算法 - 快速可解释</li>
 *   <li>贪婪+局部搜索 - 质量较高</li>
 *   <li>遗传算法 - 全局最优</li>
 *   <li>混合策略 - 自动选择</li>
 * </ul>
 *
 * @author MediAsk
 */
public interface ScheduleSolver {

    /**
     * 获取求解器名称
     */
    String name();

    /**
     * 获取求解器描述
     */
    String description();

    /**
     * 获取求解器元信息
     */
    SolverMetadata metadata();

    /**
     * 求解排班问题
     *
     * @param problem 排班问题定义
     * @return 排班解决方案
     */
    ScheduleSolution solve(ScheduleProblem problem);

    /**
     * 检查是否支持指定规模的问题
     */
    default boolean supports(ScheduleProblem problem) {
        return true;
    }
}
