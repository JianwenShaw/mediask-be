package me.jianwen.mediask.infra.schedule.engine;

import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SolverConfig;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.engine.constraint.ConstraintExpression;
import me.jianwen.mediask.schedule.domain.engine.constraint.ConstraintRule;
import me.jianwen.mediask.schedule.domain.engine.constraint.ObjectiveSpec;
import me.jianwen.mediask.schedule.domain.engine.solver.SolverPlugin;
import me.jianwen.mediask.schedule.domain.engine.solver.SolverPluginResult;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationResult;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleDoctorProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolverPluginRegistryTest {

    @Test
    void shouldFallbackToBaselineWhenCpSatUnavailable() {
        SolverPlugin greedy = new FakePlugin(SolverStrategy.RULE_GREEDY, true);
        SolverPlugin localSearch = new FakePlugin(SolverStrategy.LOCAL_SEARCH, true);
        SolverPlugin cpSat = new FakePlugin(SolverStrategy.CP_SAT, false);
        SolverPluginRegistry registry = new SolverPluginRegistry(List.of(greedy, localSearch, cpSat));

        SchedulingEngineRequest request = new SchedulingEngineRequest(
                "DEFAULT",
                200L,
                optimizationRequest(),
                List.of(new ScheduleDoctorProfile(100L, 200L, "主任医师", true)),
                List.of(),
                List.of(),
                new SolverConfig(SolverStrategy.CP_SAT, 2000, 3000L, 7L),
                null
        );
        CompiledConstraintModel model = new CompiledConstraintModel(
                "1.0",
                "abc",
                Map.of(),
                Map.of(),
                new ConstraintExpression.And(List.of()),
                new ConstraintExpression.And(List.of()),
                new ObjectiveSpec("WEIGHTED_SUM", Map.of())
        );

        SolverPluginRegistry.ResolvedSolver resolved = registry.resolve(request, model);

        assertEquals(SolverStrategy.CP_SAT, resolved.requested());
        assertEquals(SolverStrategy.RULE_GREEDY, resolved.actual());
        assertTrue(resolved.warnings().stream().anyMatch(text -> text.contains("自动回退")));
    }

    private DepartmentScheduleOptimizationRequest optimizationRequest() {
        LocalDate date = LocalDate.of(2026, 2, 20);
        return new DepartmentScheduleOptimizationRequest(
                date,
                date,
                List.of(1),
                List.of(new DepartmentScheduleDemand(200L, date, 1, 1, 0)),
                Set.of(),
                Set.of(),
                new DepartmentScheduleOptimizationRequest.HardConstraints(5, 10, 12, false, "REDUCED", 0.5D),
                new DepartmentScheduleOptimizationRequest.SoftGoals(0.3D, 0.2D, 0.15D, 0.2D, 0.15D)
        );
    }

    private record FakePlugin(SolverStrategy strategy, boolean available) implements SolverPlugin {

        @Override
        public boolean supports(SchedulingEngineRequest request, CompiledConstraintModel model) {
            return true;
        }

        @Override
        public SolverPluginResult solve(SchedulingEngineRequest request, CompiledConstraintModel model) {
            return new SolverPluginResult(
                    new DepartmentScheduleOptimizationResult(List.of(), List.of(), Map.of(), 0D, 0, List.of()),
                    List.of(),
                    List.of()
            );
        }
    }
}
