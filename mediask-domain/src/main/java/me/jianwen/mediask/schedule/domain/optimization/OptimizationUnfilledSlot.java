package me.jianwen.mediask.schedule.domain.optimization;

import java.time.LocalDate;

public record OptimizationUnfilledSlot(
        LocalDate date,
        Integer periodCode,
        Integer missingDoctors,
        String reason
) {
}
