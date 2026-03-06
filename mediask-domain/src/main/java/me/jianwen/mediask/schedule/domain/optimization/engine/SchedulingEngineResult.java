package me.jianwen.mediask.schedule.domain.optimization.engine;

import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationResult;

import java.util.List;

/**
 * 排班引擎输出（含运行元信息）。
 */
public record SchedulingEngineResult(
        DepartmentScheduleOptimizationResult result,
        SolverStrategy requestedStrategy,
        SolverStrategy actualStrategy,
        Long seed,
        List<String> minimalConflictSet,
        List<String> warnings
) {
}
