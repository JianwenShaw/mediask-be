package me.jianwen.mediask.infra.schedule.engine;

import me.jianwen.mediask.schedule.domain.engine.SchedulingEngineRequest;
import me.jianwen.mediask.schedule.domain.engine.SolverStrategy;
import me.jianwen.mediask.schedule.domain.engine.constraint.CompiledConstraintModel;
import me.jianwen.mediask.schedule.domain.engine.solver.SolverPlugin;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 求解器插件注册中心。
 */
@Component
public class SolverPluginRegistry {

    private final Map<SolverStrategy, SolverPlugin> pluginMap = new EnumMap<>(SolverStrategy.class);

    public SolverPluginRegistry(List<SolverPlugin> plugins) {
        for (SolverPlugin plugin : plugins) {
            pluginMap.put(plugin.strategy(), plugin);
        }
    }

    public ResolvedSolver resolve(SchedulingEngineRequest request, CompiledConstraintModel model) {
        SolverStrategy requested = request.solverConfig().strategy();
        if (requested == null) {
            requested = SolverStrategy.AUTO;
        }

        List<String> warnings = new ArrayList<>();
        if (requested == SolverStrategy.AUTO) {
            SolverPlugin selected = selectByScale(request, model, warnings);
            return new ResolvedSolver(requested, selected.strategy(), selected, warnings);
        }

        SolverPlugin direct = pluginMap.get(requested);
        if (direct == null || !direct.available() || !direct.supports(request, model)) {
            SolverPlugin fallback = baselinePlugin();
            warnings.add("请求求解器 %s 当前不可用，已自动回退 %s"
                    .formatted(requested.name(), fallback.strategy().name()));
            return new ResolvedSolver(requested, fallback.strategy(), fallback, warnings);
        }

        return new ResolvedSolver(requested, requested, direct, warnings);
    }

    private SolverPlugin selectByScale(
            SchedulingEngineRequest request,
            CompiledConstraintModel model,
            List<String> warnings) {
        int slots = estimateSlots(request);
        SolverPlugin preferred = slots <= 120 ? pluginMap.get(SolverStrategy.LOCAL_SEARCH) : pluginMap.get(SolverStrategy.RULE_GREEDY);
        if (preferred != null && preferred.available() && preferred.supports(request, model)) {
            return preferred;
        }
        warnings.add("AUTO 策略无法使用预期求解器，已回退 RULE_GREEDY");
        return baselinePlugin();
    }

    private int estimateSlots(SchedulingEngineRequest request) {
        long days = request.optimizationRequest().startDate().datesUntil(request.optimizationRequest().endDate().plusDays(1)).count();
        return (int) (days * Math.max(1, request.optimizationRequest().periods().size()));
    }

    private SolverPlugin baselinePlugin() {
        SolverPlugin baseline = pluginMap.get(SolverStrategy.RULE_GREEDY);
        if (baseline == null) {
            throw new IllegalStateException("Baseline solver RULE_GREEDY is not registered");
        }
        return baseline;
    }

    public record ResolvedSolver(
            SolverStrategy requested,
            SolverStrategy actual,
            SolverPlugin plugin,
            List<String> warnings
    ) {
    }
}
