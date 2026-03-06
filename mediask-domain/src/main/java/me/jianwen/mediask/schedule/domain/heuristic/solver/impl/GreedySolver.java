package me.jianwen.mediask.schedule.domain.heuristic.solver.impl;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;
import me.jianwen.mediask.schedule.domain.heuristic.problem.DateRange;
import me.jianwen.mediask.schedule.domain.heuristic.problem.Doctor;
import me.jianwen.mediask.schedule.domain.heuristic.problem.ScheduleProblem;
import me.jianwen.mediask.schedule.domain.heuristic.problem.TimeSlotConfig;
import me.jianwen.mediask.schedule.domain.heuristic.result.ScheduleSolution;
import me.jianwen.mediask.schedule.domain.heuristic.result.SolutionEvaluator;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.heuristic.solver.ScheduleSolver;
import me.jianwen.mediask.schedule.domain.heuristic.solver.SolverMetadata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 贪婪算法求解器
 *
 * <p>特点：
 * <ul>
 *   <li>快速、可解释</li>
 *   <li>按优先级依次安排每个排班时段</li>
 *   <li>适合大规模问题</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
public class GreedySolver implements ScheduleSolver {

    private static final List<ScheduleConstraint> CONSTRAINTS = List.of(
            new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.DoctorAvailabilityConstraint(),
            new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.ConsecutiveDaysConstraint(),
            new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.DailyAssignmentLimitConstraint(),
            new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.HolidayExclusionConstraint()
    );

    @Override
    public String name() {
        return "GREEDY";
    }

    @Override
    public String description() {
        return "贪婪算法 - 快速求解，按优先级选择最优医生";
    }

    @Override
    public SolverMetadata metadata() {
        return SolverMetadata.fast();
    }

    @Override
    public ScheduleSolution solve(ScheduleProblem problem) {
        long startTime = System.currentTimeMillis();
        log.info("开始贪婪算法求解，规模: {}", problem.getScaleLevel());

        try {
            SolverContext context = SolverContext.create(problem);
            DateRange dateRange = problem.getDateRange();
            TimeSlotConfig timeConfig = problem.getTimeConfig();

            // 生成所有待排班时段
            List<AssignmentSlot> slots = generateSlots(dateRange, timeConfig);

            // 按优先级排序（专家时段优先，然后是工作日）
            slots.sort(this::compareSlots);

            // 贪婪分配
            for (AssignmentSlot slot : slots) {
                Long doctorId = selectBestDoctor(slot, problem, context);
                if (doctorId != null) {
                    context.addAssignment(slot.date, slot.period, doctorId);
                    log.debug("排班: {} {} -> 医生 {}", slot.date, slot.period, doctorId);
                }
            }

            // 评估方案
            SolutionEvaluator evaluator = SolutionEvaluator.builder()
                    .constraints(CONSTRAINTS)
                    .build();
            SolutionEvaluator.EvaluationResult evalResult = evaluator.evaluate(context);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("贪婪算法完成，耗时 {}ms，评分 {}", elapsed, evalResult.getTotalScore());

            return ScheduleSolution.success(
                    name(),
                    problem,
                    context,
                    evalResult.getConstraintResults(),
                    evalResult.getTotalScore(),
                    elapsed
            );

        } catch (Exception e) {
            log.error("贪婪算法出错", e);
            return ScheduleSolution.failure(name(), problem, e.getMessage());
        }
    }

    /**
     * 生成所有待排班时段
     */
    private List<AssignmentSlot> generateSlots(DateRange dateRange, TimeSlotConfig timeConfig) {
        List<AssignmentSlot> slots = new ArrayList<>();
        for (LocalDate date : dateRange.getAllDates()) {
            if (dateRange.isWeekend(date)) {
                continue; // 跳过周末
            }
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
        // 专家时段优先
        if (a.period.equals("MORNING") && !b.period.equals("MORNING")) return -1;
        if (!a.period.equals("MORNING") && b.period.equals("MORNING")) return 1;

        // 按日期排序
        return a.date.compareTo(b.date);
    }

    /**
     * 选择最佳医生（贪婪策略）
     */
    private Long selectBestDoctor(
            AssignmentSlot slot,
            ScheduleProblem problem,
            SolverContext context) {

        List<Doctor> candidates = new ArrayList<>();

        for (Doctor doctor : problem.getDoctors()) {
            // 检查硬约束
            if (!isHardConstraintSatisfied(doctor, slot, problem, context)) {
                continue;
            }
            candidates.add(doctor);
        }

        if (candidates.isEmpty()) {
            log.warn("无法为 {} {} 找到合适的医生", slot.date, slot.period);
            return null;
        }

        // 按负载均衡选择（选择工作量最少的医生）
        candidates.sort((a, b) -> {
            int wa = context.getTotalWorkload().getOrDefault(a.getId(), 0);
            int wb = context.getTotalWorkload().getOrDefault(b.getId(), 0);
            return Integer.compare(wa, wb);
        });

        return candidates.get(0).getId();
    }

    /**
     * 检查硬约束是否满足
     */
    private boolean isHardConstraintSatisfied(
            Doctor doctor,
            AssignmentSlot slot,
            ScheduleProblem problem,
            SolverContext context) {

        // 可用性约束
        if (!doctor.isAvailableOn(slot.date)) {
            return false;
        }
        if (!doctor.isAvailableInPeriod(slot.period)) {
            return false;
        }

        // 连续工作天数约束
        if (context.getConsecutiveDays(doctor.getId()) >= problem.getMaxConsecutiveDays()) {
            return false;
        }

        // 每日排班次数约束
        if (context.getDailyAssignments(doctor.getId(), slot.date) >= problem.getMaxDailyAssignments()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean supports(ScheduleProblem problem) {
        return true; // 支持所有规模
    }

    /**
     * 待排班时段
     */
    private record AssignmentSlot(LocalDate date, String period) {}
}
