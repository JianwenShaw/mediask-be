package me.jianwen.mediask.schedule.domain.optimization.engine;

/**
 * 排班求解器运行配置。
 */
public record SolverConfig(
        SolverStrategy strategy,
        int maxIterations,
        long timeLimitMs,
        Long seed
) {
}
