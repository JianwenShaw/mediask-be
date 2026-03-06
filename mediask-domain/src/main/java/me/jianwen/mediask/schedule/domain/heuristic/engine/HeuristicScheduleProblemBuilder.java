package me.jianwen.mediask.schedule.domain.heuristic.engine;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.heuristic.problem.DateRange;
import me.jianwen.mediask.schedule.domain.heuristic.problem.Doctor;
import me.jianwen.mediask.schedule.domain.heuristic.problem.DoctorSet;
import me.jianwen.mediask.schedule.domain.heuristic.problem.ScheduleProblem;
import me.jianwen.mediask.schedule.domain.heuristic.problem.TimeSlotConfig;
import me.jianwen.mediask.schedule.domain.heuristic.solver.ScheduleSolver;
import me.jianwen.mediask.schedule.domain.heuristic.solver.SolverFactory;
import me.jianwen.mediask.schedule.domain.heuristic.result.ScheduleSolution;

import java.time.LocalDate;
import java.util.List;

/**
 * 上下文构建器
 *
 * <p>提供便捷的排班问题构建和求解入口：
 * <ul>
 *   <li>链式API构建问题</li>
 *   <li>一键执行求解</li>
 *   <li>结果格式化输出</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
public class HeuristicScheduleProblemBuilder {

    /**
     * 医生集合
     */
    private DoctorSet doctorSet;

    /**
     * 日期范围
     */
    private DateRange dateRange;

    /**
     * 时间配置
     */
    private TimeSlotConfig timeConfig = TimeSlotConfig.defaultConfig();

    /**
     * 是否需要专家覆盖
     */
    private boolean requireExpertCoverage = true;

    /**
     * 专家号最小覆盖率
     */
    private double minExpertCoverageRate = 0.3;

    /**
     * 是否启用负载均衡
     */
    private boolean enableWorkloadBalance = true;

    /**
     * 是否启用时段偏好
     */
    private boolean enableTimePreference = true;

    /**
     * 是否排除节假日
     */
    private boolean excludeHolidays = true;

    /**
     * 最大连续工作天数
     */
    private int maxConsecutiveDays = 5;

    /**
     * 每日最大排班次数
     */
    private int maxDailyAssignments = 3;

    /**
     * 求解器名称（可选，自动选择）
     */
    private String solverName;

    /**
     * 创建构建器
     */
    public static HeuristicScheduleProblemBuilder builder() {
        return new HeuristicScheduleProblemBuilder();
    }

    /**
     * 设置医生列表
     */
    public HeuristicScheduleProblemBuilder doctors(List<Doctor> doctors) {
        this.doctorSet = DoctorSet.of(doctors);
        return this;
    }

    /**
     * 设置日期范围
     */
    public HeuristicScheduleProblemBuilder dateRange(DateRange dateRange) {
        this.dateRange = dateRange;
        return this;
    }

    /**
     * 设置日期范围（天数，从明天开始）
     */
    public HeuristicScheduleProblemBuilder dateRangeDays(int days) {
        this.dateRange = DateRange.ofDays(days);
        return this;
    }

    /**
     * 设置时间配置
     */
    public HeuristicScheduleProblemBuilder timeConfig(TimeSlotConfig timeConfig) {
        this.timeConfig = timeConfig;
        return this;
    }

    /**
     * 设置求解器
     */
    public HeuristicScheduleProblemBuilder solver(String solverName) {
        this.solverName = solverName;
        return this;
    }

    /**
     * 配置专家覆盖要求
     */
    public HeuristicScheduleProblemBuilder expertCoverage(boolean require, double minRate) {
        this.requireExpertCoverage = require;
        this.minExpertCoverageRate = minRate;
        return this;
    }

    /**
     * 构建排班问题
     */
    public ScheduleProblem build() {
        validate();

        return ScheduleProblem.builder()
                .doctorSet(doctorSet)
                .dateRange(dateRange)
                .timeConfig(timeConfig)
                .requireExpertCoverage(requireExpertCoverage)
                .minExpertCoverageRate(minExpertCoverageRate)
                .enableWorkloadBalance(enableWorkloadBalance)
                .enableTimePreference(enableTimePreference)
                .excludeHolidays(excludeHolidays)
                .maxConsecutiveDays(maxConsecutiveDays)
                .maxDailyAssignments(maxDailyAssignments)
                .build();
    }

    /**
     * 验证配置
     */
    private void validate() {
        if (doctorSet == null || doctorSet.isEmpty()) {
            throw new IllegalArgumentException("医生集合不能为空");
        }
        if (dateRange == null) {
            throw new IllegalArgumentException("日期范围不能为空");
        }
    }

    /**
     * 执行求解
     */
    public ScheduleSolution solve(SolverFactory solverFactory) {
        ScheduleProblem problem = build();

        ScheduleSolver solver;
        if (solverName != null && !solverName.isEmpty()) {
            solver = solverFactory.getSolver(solverName);
        } else {
            solver = solverFactory.selectSolver(problem);
        }

        log.info("使用求解器: {}", solver.name());
        return solver.solve(problem);
    }

    /**
     * 快速求解（使用默认配置）
     */
    public static ScheduleSolution quickSolve(
            List<Doctor> doctors,
            LocalDate startDate,
            LocalDate endDate,
            SolverFactory solverFactory) {

        return HeuristicScheduleProblemBuilder.builder()
                .doctors(doctors)
                .dateRange(DateRange.of(startDate, endDate))
                .solve(solverFactory);
    }

    /**
     * 快速求解（指定天数）
     */
    public static ScheduleSolution quickSolveDays(
            List<Doctor> doctors,
            int days,
            SolverFactory solverFactory) {

        return HeuristicScheduleProblemBuilder.builder()
                .doctors(doctors)
                .dateRangeDays(days)
                .solve(solverFactory);
    }
}
