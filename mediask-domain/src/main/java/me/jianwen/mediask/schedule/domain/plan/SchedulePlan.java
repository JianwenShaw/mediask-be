package me.jianwen.mediask.schedule.domain.plan;

import java.time.LocalDate;

public record SchedulePlan(
        Long id,
        String planCode,
        Long departmentId,
        LocalDate startDate,
        LocalDate endDate,
        Integer versionNo,
        String planStatus,
        String solverStrategy,
        Long generatedBy,
        double totalScore,
        int hardViolationCount,
        String warningsJson
) {
}
