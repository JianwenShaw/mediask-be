package me.jianwen.mediask.schedule.domain.optimization.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record DepartmentScheduleOptimizationRequest(
        LocalDate startDate,
        LocalDate endDate,
        List<Integer> periods,
        List<DepartmentScheduleDemand> demands,
        Set<LocalDate> holidayDates,
        Set<LocalDate> makeupWorkdayDates,
        HardConstraints hardConstraints,
        SoftGoals softGoals
) {

    public record HardConstraints(
            int maxConsecutiveDays,
            int maxShiftsPerWeek,
            int minRestHoursBetweenShifts,
            boolean excludeHolidays,
            String holidayPolicy,
            double holidayReductionFactor
    ) {
    }

    public record SoftGoals(
            double fairnessWeight,
            double preferenceWeight,
            double continuityWeight,
            double seniorCoverageWeight,
            double weekendBalanceWeight
    ) {
    }
}
