package me.jianwen.mediask.schedule.domain.optimization.model;

import java.time.LocalDate;

public record OptimizationUnfilledSlot(
        LocalDate date,
        Integer periodCode,
        Integer missingDoctors,
        String reason
) {
}
