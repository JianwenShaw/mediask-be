package me.jianwen.mediask.infra.schedule.engine.solver;

import me.jianwen.mediask.schedule.domain.optimization.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.optimization.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.optimization.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.optimization.engine.solver.SolverPluginResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 局部搜索增强求解器（多起点策略）。
 */
@Component
public class LocalSearchSolverPlugin extends AbstractHeuristicSolverPlugin {

    public LocalSearchSolverPlugin(@Qualifier("scheduleSolverExecutor") Executor scheduleSolverExecutor) {
        super(scheduleSolverExecutor);
    }

    @Override
    public SolverStrategy strategy() {
        return SolverStrategy.LOCAL_SEARCH;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean supports(SchedulingEngineRequest request, CompiledConstraintModel model) {
        return request.doctors() != null && !request.doctors().isEmpty();
    }

    @Override
    public SolverPluginResult solve(SchedulingEngineRequest request, CompiledConstraintModel model) {
        int maxIterations = Math.max(1, request.solverConfig().maxIterations());
        int attempts = Math.min(8, Math.max(2, maxIterations / 200));
        return solveWithMultiStart(request, model, attempts);
    }
}
