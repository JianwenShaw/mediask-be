package me.jianwen.mediask.schedule.domain.heuristic.solver.impl;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.heuristic.problem.DateRange;
import me.jianwen.mediask.schedule.domain.heuristic.problem.Doctor;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;
import me.jianwen.mediask.schedule.domain.heuristic.problem.ScheduleProblem;
import me.jianwen.mediask.schedule.domain.heuristic.problem.TimeSlotConfig;
import me.jianwen.mediask.schedule.domain.heuristic.result.ScheduleSolution;
import me.jianwen.mediask.schedule.domain.heuristic.result.SolutionEvaluator;
import me.jianwen.mediask.schedule.domain.heuristic.solver.ScheduleSolver;
import me.jianwen.mediask.schedule.domain.heuristic.solver.SolverMetadata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 遗传算法求解器
 *
 * <p>特点：
 * <ul>
 *   <li>全局最优搜索</li>
 *   <li>适合复杂约束问题</li>
 *   <li>计算开销较大</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
public class GeneticAlgorithmSolver implements ScheduleSolver {

    // 遗传算法参数
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.1;
    private static final double ELITE_RATE = 0.2;
    private static final int TOURNAMENT_SIZE = 5;

    @Override
    public String name() {
        return "GENETIC";
    }

    @Override
    public String description() {
        return "遗传算法 - 全局搜索，适合复杂约束问题";
    }

    @Override
    public SolverMetadata metadata() {
        return SolverMetadata.highQuality();
    }

    @Override
    public ScheduleSolution solve(ScheduleProblem problem) {
        long startTime = System.currentTimeMillis();
        log.info("开始遗传算法求解，规模: {}", problem.getScaleLevel());

        try {
            // 生成所有排班槽位
            List<AssignmentSlot> slots = generateSlots(problem);
            List<Long> doctorIds = problem.getDoctors().stream()
                    .map(Doctor::getId)
                    .toList();

            // 初始化种群
            List<Chromosome> population = initializePopulation(problem, slots, doctorIds);

            // 评估种群
            SolutionEvaluator evaluator = SolutionEvaluator.defaultEvaluator();
            evaluatePopulation(population, evaluator, problem);

            Chromosome best = getBest(population);
            double bestScore = best.fitness;

            log.info("初始最佳评分: {}", bestScore);

            // 进化
            int generation = 0;
            for (generation = 0; generation < MAX_GENERATIONS; generation++) {
                // 选择
                List<Chromosome> selected = selection(population);

                // 交叉
                List<Chromosome> offspring = crossover(selected, slots.size());

                // 变异
                mutate(offspring, slots, doctorIds, problem);

                // 评估
                evaluatePopulation(offspring, evaluator, problem);

                // 合并种群（精英保留）
                population = mergePopulations(population, offspring);

                // 更新最佳
                Chromosome currentBest = getBest(population);
                if (currentBest.fitness > bestScore) {
                    bestScore = currentBest.fitness;
                    best = currentBest;
                    log.debug("第 {} 代找到更好的解，评分: {}", generation + 1, bestScore);
                }

                // 早停条件
                if (bestScore >= 100) {
                    log.info("达到最优解，提前停止");
                    break;
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("遗传算法完成，进化 {} 代，评分 {}，耗时 {}ms",
                    generation + 1, bestScore, elapsed);

            // 构建解
            SolverContext context = buildContext(best, slots, problem);
            return ScheduleSolution.success(
                    name(),
                    problem,
                    context,
                    evaluator.evaluate(context).getConstraintResults(),
                    bestScore,
                    elapsed
            );

        } catch (Exception e) {
            log.error("遗传算法出错", e);
            return ScheduleSolution.failure(name(), problem, e.getMessage());
        }
    }

    /**
     * 生成排班槽位
     */
    private List<AssignmentSlot> generateSlots(ScheduleProblem problem) {
        DateRange dateRange = problem.getDateRange();
        TimeSlotConfig timeConfig = problem.getTimeConfig();
        List<AssignmentSlot> slots = new ArrayList<>();

        for (LocalDate date : dateRange.getAllDates()) {
            if (dateRange.isWeekend(date)) continue;
            for (String period : timeConfig.getEnabledPeriods()) {
                slots.add(new AssignmentSlot(date, period));
            }
        }
        return slots;
    }

    /**
     * 初始化种群
     */
    private List<Chromosome> initializePopulation(
            ScheduleProblem problem,
            List<AssignmentSlot> slots,
            List<Long> doctorIds) {

        List<Chromosome> population = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            int[] genes = new int[slots.size()];
            for (int j = 0; j < slots.size(); j++) {
                genes[j] = random.nextInt(doctorIds.size());
            }
            population.add(new Chromosome(genes));
        }

        return population;
    }

    /**
     * 评估种群
     */
    private void evaluatePopulation(
            List<Chromosome> population,
            SolutionEvaluator evaluator,
            ScheduleProblem problem) {

        for (Chromosome chrom : population) {
            SolverContext context = buildContext(chrom, null, problem);
            SolutionEvaluator.EvaluationResult result = evaluator.evaluate(context);
            chrom.fitness = result.getTotalScore();
        }
    }

    /**
     * 选择（锦标赛选择）
     */
    private List<Chromosome> selection(List<Chromosome> population) {
        List<Chromosome> selected = new ArrayList<>();
        Random random = new Random();
        int eliteCount = (int) (population.size() * ELITE_RATE);

        // 保留精英
        List<Chromosome> sorted = new ArrayList<>(population);
        sorted.sort((a, b) -> Double.compare(b.fitness, a.fitness));
        for (int i = 0; i < eliteCount; i++) {
            selected.add(sorted.get(i).copy());
        }

        // 锦标赛选择
        while (selected.size() < population.size()) {
            Chromosome best = null;
            for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                Chromosome candidate = population.get(random.nextInt(population.size()));
                if (best == null || candidate.fitness > best.fitness) {
                    best = candidate;
                }
            }
            selected.add(best.copy());
        }

        return selected;
    }

    /**
     * 交叉（单点交叉）
     */
    private List<Chromosome> crossover(List<Chromosome> parents, int geneCount) {
        List<Chromosome> offspring = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < parents.size() / 2; i++) {
            Chromosome p1 = parents.get(i * 2);
            Chromosome p2 = parents.get(i * 2 + 1);

            int[] child1 = new int[geneCount];
            int[] child2 = new int[geneCount];

            int crossoverPoint = random.nextInt(geneCount);
            for (int j = 0; j < geneCount; j++) {
                if (j < crossoverPoint) {
                    child1[j] = p1.genes[j];
                    child2[j] = p2.genes[j];
                } else {
                    child1[j] = p2.genes[j];
                    child2[j] = p1.genes[j];
                }
            }

            offspring.add(new Chromosome(child1));
            if (offspring.size() < parents.size()) {
                offspring.add(new Chromosome(child2));
            }
        }

        return offspring;
    }

    /**
     * 变异
     */
    private void mutate(
            List<Chromosome> offspring,
            List<AssignmentSlot> slots,
            List<Long> doctorIds,
            ScheduleProblem problem) {

        Random random = new Random();
        int mutationCount = (int) (offspring.size() * slots.size() * MUTATION_RATE);

        for (int i = 0; i < mutationCount; i++) {
            int chromIdx = random.nextInt(offspring.size());
            int geneIdx = random.nextInt(slots.size());

            // 随机变异为另一个医生
            int currentGene = offspring.get(chromIdx).genes[geneIdx];
            int newGene;
            do {
                newGene = random.nextInt(doctorIds.size());
            } while (newGene == currentGene);

            offspring.get(chromIdx).genes[geneIdx] = newGene;
        }
    }

    /**
     * 合并种群
     */
    private List<Chromosome> mergePopulations(
            List<Chromosome> population,
            List<Chromosome> offspring) {

        List<Chromosome> merged = new ArrayList<>(population);
        merged.addAll(offspring);
        merged.sort((a, b) -> Double.compare(b.fitness, a.fitness));
        return merged.subList(0, POPULATION_SIZE);
    }

    /**
     * 获取最佳个体
     */
    private Chromosome getBest(List<Chromosome> population) {
        return population.stream()
                .max(Comparator.comparingDouble(c -> c.fitness))
                .orElseThrow();
    }

    /**
     * 构建上下文
     */
    private SolverContext buildContext(
            Chromosome chrom,
            List<AssignmentSlot> slots,
            ScheduleProblem problem) {

        if (slots == null) {
            slots = generateSlots(problem);
        }

        SolverContext context = SolverContext.create(problem);
        List<Long> doctorIds = problem.getDoctors().stream()
                .map(Doctor::getId)
                .toList();

        for (int i = 0; i < slots.size() && i < chrom.genes.length; i++) {
            AssignmentSlot slot = slots.get(i);
            int gene = chrom.genes[i];
            if (gene >= 0 && gene < doctorIds.size()) {
                context.addAssignment(slot.date, slot.period, doctorIds.get(gene));
            }
        }

        return context;
    }

    @Override
    public boolean supports(ScheduleProblem problem) {
        return "SMALL".equals(problem.getScaleLevel()) ||
                (problem.estimateTotalAssignments() < 200 &&
                 problem.getDoctors().size() < 20);
    }

    private record AssignmentSlot(LocalDate date, String period) {}

    /**
     * 染色体（个体）
     */
    private static class Chromosome {
        int[] genes;
        double fitness;

        Chromosome(int[] genes) {
            this.genes = genes;
            this.fitness = 0;
        }

        Chromosome copy() {
            int[] copy = genes.clone();
            Chromosome c = new Chromosome(copy);
            c.fitness = this.fitness;
            return c;
        }
    }
}
