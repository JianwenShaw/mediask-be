package me.jianwen.mediask.schedule.domain.optimization.model;

import java.util.List;
import java.util.Map;

public record DepartmentScheduleOptimizationResult(
        List<OptimizationAssignment> assignments,
        List<OptimizationUnfilledSlot> unfilledSlots,
        Map<String, Double> scoreBreakdown,
        double totalScore,
        int hardViolationCount,
        List<String> warnings
) {
}
