package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 科室多医生自动排班命令
 */
@Data
public class AutoScheduleCommand {

    public static final int DEFAULT_MAX_CONSECUTIVE_DAYS = 5;
    public static final int DEFAULT_MAX_SHIFTS_PER_WEEK = 10;
    public static final int DEFAULT_MIN_REST_HOURS = 12;
    public static final boolean DEFAULT_EXCLUDE_HOLIDAYS = false;
    public static final String DEFAULT_HOLIDAY_POLICY = "REDUCED";
    public static final double DEFAULT_HOLIDAY_REDUCTION_FACTOR = 0.50D;

    public static final double DEFAULT_FAIRNESS_WEIGHT = 0.30D;
    public static final double DEFAULT_PREFERENCE_WEIGHT = 0.20D;
    public static final double DEFAULT_CONTINUITY_WEIGHT = 0.15D;
    public static final double DEFAULT_SENIOR_COVERAGE_WEIGHT = 0.20D;
    public static final double DEFAULT_WEEKEND_BALANCE_WEIGHT = 0.15D;

    public static final String DEFAULT_SOLVER_STRATEGY = "AUTO";
    public static final int DEFAULT_MAX_ITERATIONS = 2000;
    public static final long DEFAULT_TIME_LIMIT_MS = 3000L;

    private Long departmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Long> doctorIds;
    private List<Integer> periods;
    private List<DemandItemCommand> demands;
    private HardConstraints hardConstraints;
    private SoftGoals softGoals;
    private SolverConfig solverConfig;
    private String constraintDslJson;
    private String ruleProfileCode;
    private Long basePlanId;
    private LocalDate replanWindowStartDate;
    private LocalDate replanWindowEndDate;

    public HardConstraints resolvedHardConstraints() {
        if (hardConstraints == null) {
            return new HardConstraints(
                    DEFAULT_MAX_CONSECUTIVE_DAYS,
                    DEFAULT_MAX_SHIFTS_PER_WEEK,
                    DEFAULT_MIN_REST_HOURS,
                    DEFAULT_EXCLUDE_HOLIDAYS,
                    DEFAULT_HOLIDAY_POLICY,
                    DEFAULT_HOLIDAY_REDUCTION_FACTOR
            );
        }
        return new HardConstraints(
                valueOrDefault(hardConstraints.maxConsecutiveDays(), DEFAULT_MAX_CONSECUTIVE_DAYS),
                valueOrDefault(hardConstraints.maxShiftsPerWeek(), DEFAULT_MAX_SHIFTS_PER_WEEK),
                valueOrDefault(hardConstraints.minRestHoursBetweenShifts(), DEFAULT_MIN_REST_HOURS),
                boolOrDefault(hardConstraints.excludeHolidays(), DEFAULT_EXCLUDE_HOLIDAYS),
                hardConstraints.holidayPolicy() == null ? DEFAULT_HOLIDAY_POLICY : hardConstraints.holidayPolicy(),
                valueOrDefault(hardConstraints.holidayReductionFactor(), DEFAULT_HOLIDAY_REDUCTION_FACTOR)
        );
    }

    public SoftGoals resolvedSoftGoals() {
        if (softGoals == null) {
            return new SoftGoals(
                    DEFAULT_FAIRNESS_WEIGHT,
                    DEFAULT_PREFERENCE_WEIGHT,
                    DEFAULT_CONTINUITY_WEIGHT,
                    DEFAULT_SENIOR_COVERAGE_WEIGHT,
                    DEFAULT_WEEKEND_BALANCE_WEIGHT
            );
        }
        return new SoftGoals(
                valueOrDefault(softGoals.fairnessWeight(), DEFAULT_FAIRNESS_WEIGHT),
                valueOrDefault(softGoals.preferenceWeight(), DEFAULT_PREFERENCE_WEIGHT),
                valueOrDefault(softGoals.continuityWeight(), DEFAULT_CONTINUITY_WEIGHT),
                valueOrDefault(softGoals.seniorCoverageWeight(), DEFAULT_SENIOR_COVERAGE_WEIGHT),
                valueOrDefault(softGoals.weekendBalanceWeight(), DEFAULT_WEEKEND_BALANCE_WEIGHT)
        );
    }

    public SolverConfig resolvedSolverConfig() {
        if (solverConfig == null) {
            return new SolverConfig(DEFAULT_SOLVER_STRATEGY, DEFAULT_MAX_ITERATIONS, DEFAULT_TIME_LIMIT_MS, null);
        }
        return new SolverConfig(
                normalizeStrategy(solverConfig.strategy()),
                valueOrDefault(solverConfig.maxIterations(), DEFAULT_MAX_ITERATIONS),
                valueOrDefault(solverConfig.timeLimitMs(), DEFAULT_TIME_LIMIT_MS),
                solverConfig.seed()
        );
    }

    private static String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return DEFAULT_SOLVER_STRATEGY;
        }
        return strategy.trim().toUpperCase(Locale.ROOT);
    }

    public boolean incrementalReplan() {
        return basePlanId != null;
    }

    public LocalDate resolvedPlanningStartDate() {
        return incrementalReplan() && replanWindowStartDate != null ? replanWindowStartDate : startDate;
    }

    public LocalDate resolvedPlanningEndDate() {
        return incrementalReplan() && replanWindowEndDate != null ? replanWindowEndDate : endDate;
    }

    private static int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static long valueOrDefault(Long value, long defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static double valueOrDefault(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static boolean boolOrDefault(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    public record DemandItemCommand(LocalDate date, Integer periodCode, Integer requiredDoctors, Integer minSeniorDoctors) {
    }

    public record HardConstraints(
            Integer maxConsecutiveDays,
            Integer maxShiftsPerWeek,
            Integer minRestHoursBetweenShifts,
            Boolean excludeHolidays,
            String holidayPolicy,
            Double holidayReductionFactor
    ) {
    }

    public record SoftGoals(
            Double fairnessWeight,
            Double preferenceWeight,
            Double continuityWeight,
            Double seniorCoverageWeight,
            Double weekendBalanceWeight
    ) {
    }

    public record SolverConfig(
            String strategy,
            Integer maxIterations,
            Long timeLimitMs,
            Long seed
    ) {
    }
}
