package me.jianwen.mediask.infra.schedule.engine.solver;

import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.engine.solver.SolverPlugin;
import me.jianwen.mediask.schedule.domain.engine.solver.SolverPluginResult;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OR-Tools CP-SAT 预留插件。
 */
@Component
public class OrToolsCpSatSolverPlugin implements SolverPlugin {

    @Override
    public SolverStrategy strategy() {
        return SolverStrategy.CP_SAT;
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean supports(SchedulingEngineRequest request, CompiledConstraintModel model) {
        return false;
    }

    @Override
    public SolverPluginResult solve(SchedulingEngineRequest request, CompiledConstraintModel model) {
        DepartmentScheduleOptimizationResult result = new DepartmentScheduleOptimizationResult(
                List.of(),
                List.of(),
                Map.of(),
                0D,
                1,
                List.of("CP_SAT 插件尚未启用")
        );
        return new SolverPluginResult(result, List.of("CP_SAT 插件尚未启用"), List.of("CP_SAT_NOT_ENABLED"));
    }
}
