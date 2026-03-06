package me.jianwen.mediask.infra.schedule.engine;

import me.jianwen.mediask.infra.schedule.engine.dsl.ConstraintDslCompiler;
import me.jianwen.mediask.schedule.domain.optimization.engine.SchedulingEngine;
import me.jianwen.mediask.schedule.domain.optimization.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.optimization.engine.SchedulingEngineResult;
import me.jianwen.mediask.schedule.domain.optimization.engine.solver.SolverPluginResult;
import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认排班引擎实现。
 */
@Component
public class DefaultSchedulingEngine implements SchedulingEngine {

    private final ConstraintDslCompiler dslCompiler;
    private final SolverPluginRegistry solverPluginRegistry;

    public DefaultSchedulingEngine(
            ConstraintDslCompiler dslCompiler,
            SolverPluginRegistry solverPluginRegistry) {
        this.dslCompiler = dslCompiler;
        this.solverPluginRegistry = solverPluginRegistry;
    }

    @Override
    public SchedulingEngineResult optimize(SchedulingEngineRequest request) {
        var model = dslCompiler.compile(request);
        var resolvedSolver = solverPluginRegistry.resolve(request, model);
        SolverPluginResult solverResult = resolvedSolver.plugin().solve(request, model);

        DepartmentScheduleOptimizationResult raw = solverResult.result();
        List<String> mergedWarnings = mergeWarnings(raw.warnings(), resolvedSolver.warnings(), solverResult.warnings());

        DepartmentScheduleOptimizationResult finalResult = new DepartmentScheduleOptimizationResult(
                raw.assignments(),
                raw.unfilledSlots(),
                raw.scoreBreakdown(),
                raw.totalScore(),
                raw.hardViolationCount(),
                mergedWarnings
        );

        return new SchedulingEngineResult(
                finalResult,
                resolvedSolver.requested(),
                resolvedSolver.actual(),
                request.solverConfig().seed(),
                solverResult.minimalConflictSet(),
                mergedWarnings
        );
    }

    private List<String> mergeWarnings(List<String>... warningLists) {
        Set<String> merged = new LinkedHashSet<>();
        for (List<String> warningList : warningLists) {
            if (warningList == null || warningList.isEmpty()) {
                continue;
            }
            merged.addAll(warningList);
        }
        return new ArrayList<>(merged);
    }
}
