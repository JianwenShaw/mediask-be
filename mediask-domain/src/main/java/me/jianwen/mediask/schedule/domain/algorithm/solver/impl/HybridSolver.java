package me.jianwen.mediask.schedule.domain.algorithm.solver.impl;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.algorithm.problem.*;
import me.jianwen.mediask.schedule.domain.algorithm.result.ScheduleSolution;
import me.jianwen.mediask.schedule.domain.algorithm.result.SolutionEvaluator;
import me.jianwen.mediask.schedule.domain.algorithm.solver.ScheduleSolver;
import me.jianwen.mediask.schedule.domain.algorithm.solver.SolverFactory;
import me.jianwen.mediask.schedule.domain.algorithm.solver.SolverMetadata;
import org.springframework.stereotype.Component;

/**
 * 混合策略求解器
 *
 * <p>特点：
 * <ul>
 *   <li>自动选择最优算法</li>
 *   <li>可配置执行策略</li>
 *   <li>适应不同问题规模</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
@Component
public class HybridSolver implements ScheduleSolver {

    private final SolverFactory solverFactory;

    public HybridSolver(SolverFactory solverFactory) {
        this.solverFactory = solverFactory;
    }

    @Override
    public String name() {
        return "HYBRID";
    }

    @Override
    public String description() {
        return "混合策略 - 自动选择最优算法";
    }

    @Override
    public SolverMetadata metadata() {
        return SolverMetadata.balanced();
    }

    @Override
    public ScheduleSolution solve(ScheduleProblem problem) {
        long startTime = System.currentTimeMillis();
        log.info("开始混合策略求解，规模: {}", problem.getScaleLevel());

        try {
            // 根据问题规模选择求解器
            ScheduleSolver primarySolver = selectSolver(problem);
            log.info("选择主求解器: {}", primarySolver.name());

            // 执行主求解
            ScheduleSolution primarySolution = primarySolver.solve(problem);

            if (!primarySolution.isSuccess()) {
                return primarySolution;
            }

            // 如果评分不够好，尝试其他求解器
            if (primarySolution.getScore() < 80) {
                log.info("主求解评分较低，尝试其他求解器...");
                ScheduleSolution backupSolution = tryBackupSolvers(problem, primarySolution.getScore());
                if (backupSolution.isSuccess() && backupSolution.getScore() > primarySolution.getScore()) {
                    log.info("找到更好的解: {} -> {}",
                            primarySolution.getScore(), backupSolution.getScore());
                    return backupSolution;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("混合策略完成，使用 {}, 评分 {}, 耗时 {}ms",
                    primarySolution.getSolverName(),
                    primarySolution.getScore(),
                    elapsed);

            return primarySolution;

        } catch (Exception e) {
            log.error("混合策略出错", e);
            return ScheduleSolution.failure(name(), problem, e.getMessage());
        }
    }

    /**
     * 根据问题选择求解器
     */
    private ScheduleSolver selectSolver(ScheduleProblem problem) {
        String scale = problem.getScaleLevel();
        int estimatedAssignments = problem.estimateTotalAssignments();
        int doctorCount = problem.getDoctors().size();

        // 小规模 + 少医生 -> 遗传算法
        if ("SMALL".equals(scale) && doctorCount <= 10 && estimatedAssignments < 100) {
            return solverFactory.getSolver("GENETIC");
        }

        // 中等规模 -> 贪婪局部搜索
        if ("MEDIUM".equals(scale) || estimatedAssignments < 500) {
            return solverFactory.getSolver("GREEDY_LOCAL_SEARCH");
        }

        // 大规模 -> 贪婪算法
        return solverFactory.getSolver("GREEDY");
    }

    /**
     * 尝试备用求解器
     */
    private ScheduleSolution tryBackupSolvers(ScheduleProblem problem, double currentScore) {
        ScheduleSolution bestSolution = null;
        double bestScore = currentScore;

        // 尝试其他求解器
        for (String solverName : solverFactory.getSolverNames()) {
            if (solverName.equals(name())) continue;

            ScheduleSolver solver = solverFactory.getSolver(solverName);
            if (!solver.supports(problem)) continue;

            log.info("尝试求解器: {}", solverName);
            ScheduleSolution solution = solver.solve(problem);

            if (solution.isSuccess() && solution.getScore() > bestScore) {
                bestScore = solution.getScore();
                bestSolution = solution;
                log.info("找到更好的解: {}", bestScore);
            }
        }

        return bestSolution;
    }

    @Override
    public boolean supports(ScheduleProblem problem) {
        return true;
    }
}
