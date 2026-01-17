package me.jianwen.mediask.schedule.domain.algorithm.solver.impl;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.algorithm.problem.*;
import me.jianwen.mediask.schedule.domain.algorithm.result.ScheduleSolution;
import me.jianwen.mediask.schedule.domain.algorithm.result.SolutionEvaluator;
import me.jianwen.mediask.schedule.domain.algorithm.solver.ScheduleSolver;
import me.jianwen.mediask.schedule.domain.algorithm.solver.SolverMetadata;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

/**
 * 贪婪+局部搜索求解器
 *
 * <p>特点：
 * <ul>
 *   <li>质量较高（默认求解器）</li>
 *   <li>先贪婪初始化，再局部搜索优化</li>
 *   <li>适用于中小规模问题</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
@Component
public class GreedyLocalSearchSolver implements ScheduleSolver {

    private static final int MAX_ITERATIONS = 1000;
    private static final double IMPROVEMENT_THRESHOLD = 0.01;

    @Override
    public String name() {
        return "GREEDY_LOCAL_SEARCH";
    }

    @Override
    public String description() {
        return "贪婪+局部搜索 - 先初始化后优化，质量较高";
    }

    @Override
    public SolverMetadata metadata() {
        return SolverMetadata.balanced();
    }

    @Override
    public ScheduleSolution solve(ScheduleProblem problem) {
        long startTime = System.currentTimeMillis();
        log.info("开始贪婪局部搜索求解，规模: {}", problem.getScaleLevel());

        try {
            // 第一阶段：贪婪初始化
            ScheduleContext currentContext = greedyInitialize(problem);

            // 评估初始解
            SolutionEvaluator evaluator = SolutionEvaluator.defaultEvaluator();
            SolutionEvaluator.EvaluationResult currentEval = evaluator.evaluate(currentContext);
            double currentScore = currentEval.getTotalScore();

            log.info("初始解评分: {}", currentScore);

            // 第二阶段：局部搜索优化
            int iterations = 0;
            double bestScore = currentScore;
            ScheduleContext bestContext = currentContext.copy();

            while (iterations < MAX_ITERATIONS) {
                iterations++;

                // 生成邻居解
                ScheduleContext neighbor = generateNeighbor(currentContext, problem);

                // 评估邻居解
                SolutionEvaluator.EvaluationResult neighborEval = evaluator.evaluate(neighbor);
                double neighborScore = neighborEval.getTotalScore();

                // 如果邻居解更好，则接受
                if (neighborScore > currentScore + IMPROVEMENT_THRESHOLD) {
                    currentContext = neighbor;
                    currentScore = neighborScore;

                    if (neighborScore > bestScore) {
                        bestScore = neighborScore;
                        bestContext = neighbor.copy();
                        log.debug("找到更好的解，评分: {}", bestScore);
                    }
                } else {
                    // 以一定概率接受较差的解（模拟退火思想）
                    if (Math.random() < 0.1) {
                        currentContext = neighbor;
                        currentScore = neighborScore;
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("贪婪局部搜索完成，迭代{}次，评分 {}，耗时 {}ms",
                    iterations, bestScore, elapsed);

            return ScheduleSolution.success(
                    name(),
                    problem,
                    bestContext,
                    evaluator.evaluate(bestContext).getConstraintResults(),
                    bestScore,
                    elapsed
            );

        } catch (Exception e) {
            log.error("贪婪局部搜索出错", e);
            return ScheduleSolution.failure(name(), problem, e.getMessage());
        }
    }

    /**
     * 贪婪初始化
     */
    private ScheduleContext greedyInitialize(ScheduleProblem problem) {
        ScheduleContext context = ScheduleContext.create(problem);
        DateRange dateRange = problem.getDateRange();
        TimeSlotConfig timeConfig = problem.getTimeConfig();

        List<AssignmentSlot> slots = generateSlots(dateRange, timeConfig);
        slots.sort(this::compareSlots);

        for (AssignmentSlot slot : slots) {
            Long doctorId = selectBestDoctor(slot, problem, context);
            if (doctorId != null) {
                context.addAssignment(slot.date, slot.period, doctorId);
            }
        }

        return context;
    }

    /**
     * 生成待排班时段
     */
    private List<AssignmentSlot> generateSlots(DateRange dateRange, TimeSlotConfig timeConfig) {
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
     * 比较时段优先级
     */
    private int compareSlots(AssignmentSlot a, AssignmentSlot b) {
        if (a.period.equals("MORNING") && !b.period.equals("MORNING")) return -1;
        if (!a.period.equals("MORNING") && b.period.equals("MORNING")) return 1;
        return a.date.compareTo(b.date);
    }

    /**
     * 选择最佳医生（考虑软约束）
     */
    private Long selectBestDoctor(
            AssignmentSlot slot,
            ScheduleProblem problem,
            ScheduleContext context) {

        List<ScoredDoctor> candidates = new ArrayList<>();

        for (Doctor doctor : problem.getDoctors()) {
            if (!isAvailable(doctor, slot, problem, context)) {
                continue;
            }

            // 计算得分（负载均衡优先，然后是偏好匹配）
            double score = calculateDoctorScore(doctor, slot, problem, context);
            candidates.add(new ScoredDoctor(doctor.getId(), score));
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // 按得分排序
        candidates.sort((a, b) -> Double.compare(b.score, a.score));
        return candidates.get(0).doctorId;
    }

    /**
     * 检查医生是否可用
     */
    private boolean isAvailable(
            Doctor doctor,
            AssignmentSlot slot,
            ScheduleProblem problem,
            ScheduleContext context) {

        if (!doctor.isAvailableOn(slot.date)) return false;
        if (!doctor.isAvailableInPeriod(slot.period)) return false;
        if (context.getConsecutiveDays(doctor.getId()) >= problem.getMaxConsecutiveDays()) return false;
        if (context.getDailyAssignments(doctor.getId(), slot.date) >= problem.getMaxDailyAssignments()) return false;

        return true;
    }

    /**
     * 计算医生得分
     */
    private double calculateDoctorScore(
            Doctor doctor,
            AssignmentSlot slot,
            ScheduleProblem problem,
            ScheduleContext context) {

        double score = 0;

        // 负载均衡得分（工作量越少得分越高）
        int workload = context.getTotalWorkload().getOrDefault(doctor.getId(), 0);
        score += (100 - workload * 5);

        // 偏好匹配得分
        if (doctor.prefersPeriod(slot.period)) {
            score += 20;
        }

        // 专家优先（如果需要专家覆盖）
        if (problem.isRequireExpertCoverage() && doctor.isExpert()) {
            score += 15;
        }

        return score;
    }

    /**
     * 生成邻居解（交换两个排班位置的医生）
     */
    private ScheduleContext generateNeighbor(ScheduleContext current, ScheduleProblem problem) {
        ScheduleContext neighbor = current.copy();
        var assignments = new HashMap<>(neighbor.getAssignments());

        if (assignments.size() < 2) {
            return neighbor;
        }

        // 随机选择两个位置交换
        List<String> keys = new ArrayList<>(assignments.keySet());
        Collections.shuffle(keys);

        if (keys.size() >= 2) {
            String key1 = keys.get(0);
            String key2 = keys.get(1);

            Long doctor1 = assignments.get(key1);
            Long doctor2 = assignments.get(key2);

            // 交换
            assignments.put(key1, doctor2);
            assignments.put(key2, doctor1);

            neighbor.setAssignments(assignments);
        }

        return neighbor;
    }

    @Override
    public boolean supports(ScheduleProblem problem) {
        return !"LARGE".equals(problem.getScaleLevel());
    }

    private record AssignmentSlot(LocalDate date, String period) {}
    private record ScoredDoctor(Long doctorId, double score) {}
}
