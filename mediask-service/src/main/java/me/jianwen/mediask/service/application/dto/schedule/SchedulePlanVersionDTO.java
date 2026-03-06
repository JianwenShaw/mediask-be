package me.jianwen.mediask.service.application.dto.schedule;

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
