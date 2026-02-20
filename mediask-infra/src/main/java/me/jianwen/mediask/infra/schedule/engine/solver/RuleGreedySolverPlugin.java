package me.jianwen.mediask.infra.schedule.engine.solver;

import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.engine.solver.SolverPluginResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * 基线求解器（规则贪心）。
 */
@Component
public class RuleGreedySolverPlugin extends AbstractHeuristicSolverPlugin {

    public RuleGreedySolverPlugin(@Qualifier("scheduleSolverExecutor") Executor scheduleSolverExecutor) {
        super(scheduleSolverExecutor);
    }

    @Override
    public SolverStrategy strategy() {
        return SolverStrategy.RULE_GREEDY;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean supports(SchedulingEngineRequest request, CompiledConstraintModel model) {
        return true;
    }

    @Override
    public SolverPluginResult solve(SchedulingEngineRequest request, CompiledConstraintModel model) {
        return solveWithMultiStart(request, model, 1);
    }
}
