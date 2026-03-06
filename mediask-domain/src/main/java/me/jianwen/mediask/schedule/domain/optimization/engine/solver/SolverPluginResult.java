package me.jianwen.mediask.schedule.domain.optimization.engine.solver;

import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationResult;

import java.util.List;

/**
 * 求解器执行结果。
 */
public record SolverPluginResult(
        DepartmentScheduleOptimizationResult result,
        List<String> warnings,
        List<String> minimalConflictSet
) {
}
