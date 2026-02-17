package me.jianwen.mediask.common.dto.schedule;

import java.time.LocalDate;

public record SchedulePlanVersionDTO(
        Long planId,
        String planCode,
        Integer versionNo,
        String planStatus,
        LocalDate startDate,
        LocalDate endDate,
        double totalScore,
        int hardViolationCount
) {
}
