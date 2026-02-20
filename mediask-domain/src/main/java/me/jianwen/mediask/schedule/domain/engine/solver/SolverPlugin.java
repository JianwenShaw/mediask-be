package me.jianwen.mediask.schedule.domain.engine.solver;

import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.engine.constraint.CompiledConstraintModel;

/**
 * 可插拔求解器接口。
 */
public interface SolverPlugin {

    SolverStrategy strategy();

    boolean available();

    boolean supports(SchedulingEngineRequest request, CompiledConstraintModel model);

    SolverPluginResult solve(SchedulingEngineRequest request, CompiledConstraintModel model);
}
