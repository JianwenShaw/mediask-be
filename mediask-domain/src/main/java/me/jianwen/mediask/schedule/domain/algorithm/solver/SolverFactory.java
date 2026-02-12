package me.jianwen.mediask.schedule.domain.algorithm.solver;

import me.jianwen.mediask.schedule.domain.algorithm.problem.ScheduleProblem;
import me.jianwen.mediask.schedule.domain.algorithm.solver.impl.GeneticAlgorithmSolver;
import me.jianwen.mediask.schedule.domain.algorithm.solver.impl.GreedyLocalSearchSolver;
import me.jianwen.mediask.schedule.domain.algorithm.solver.impl.GreedySolver;
import me.jianwen.mediask.schedule.domain.algorithm.solver.impl.HybridSolver;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 求解器工厂
 *
 * <p>提供求解器的获取和自动选择功能：
 * <ul>
 *   <li>根据名称获取求解器</li>
 *   <li>根据问题规模自动选择最优求解器</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
public class SolverFactory {

    private final Map<String, ScheduleSolver> solvers = new HashMap<>();

    /**
     * 构造函数 - 注册所有求解器
     * HybridSolver 由调用方注入
     */
    public SolverFactory(
            GreedySolver greedySolver,
            GreedyLocalSearchSolver greedyLocalSearchSolver,
            GeneticAlgorithmSolver geneticAlgorithmSolver,
            HybridSolver hybridSolver) {

        register(greedySolver);
        register(greedyLocalSearchSolver);
        register(geneticAlgorithmSolver);
        register(hybridSolver);

        log.info("注册了 {} 个排班求解器: {}", solvers.size(), solvers.keySet());
    }

    /**
     * 注册求解器
     */
    private void register(ScheduleSolver solver) {
        solvers.put(solver.name().toUpperCase(), solver);
    }

    /**
     * 根据名称获取求解器
     *
     * @param name 求解器名称
     * @return 求解器实例
     */
    public ScheduleSolver getSolver(String name) {
        if (name == null) {
            return getDefaultSolver();
        }
        ScheduleSolver solver = solvers.get(name.toUpperCase());
        if (solver == null) {
            log.warn("未找到求解器 '{}'，使用默认求解器", name);
            return getDefaultSolver();
        }
        return solver;
    }

    /**
     * 获取默认求解器
     */
    public ScheduleSolver getDefaultSolver() {
        return solvers.get("GREEDY_LOCAL_SEARCH");
    }

    /**
     * 根据问题自动选择最优求解器
     *
     * @param problem 排班问题
     * @return 推荐的求解器
     */
    public ScheduleSolver selectSolver(ScheduleProblem problem) {
        String scale = problem.getScaleLevel();

        // 小规模问题使用高质量算法
        if ("SMALL".equals(scale)) {
            if (problem.estimateTotalAssignments() < 100) {
                log.debug("小规模问题，使用遗传算法");
                return solvers.get("GENETIC");
            }
            log.debug("小规模问题，使用贪婪局部搜索");
            return solvers.get("GREEDY_LOCAL_SEARCH");
        }

        // 大规模问题使用快速算法
        if ("LARGE".equals(scale)) {
            log.debug("大规模问题，使用贪婪算法");
            return solvers.get("GREEDY");
        }

        // 中等规模问题使用平衡算法
        log.debug("中等规模问题，使用混合策略");
        return solvers.get("HYBRID");
    }

    /**
     * 获取所有可用的求解器
     */
    public List<ScheduleSolver> getAllSolvers() {
        return List.copyOf(solvers.values());
    }

    /**
     * 获取求解器名称列表
     */
    public List<String> getSolverNames() {
        return solvers.keySet().stream().toList();
    }

    /**
     * 检查求解器是否存在
     */
    public boolean hasSolver(String name) {
        return solvers.containsKey(name.toUpperCase());
    }
}
