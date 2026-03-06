package me.jianwen.mediask.schedule.domain.optimization;

import java.time.LocalDate;
import java.util.List;

public record OptimizationAssignment(
        LocalDate date,
        Integer periodCode,
        Long doctorId,
        List<String> reasons,
        List<String> penalties
) {
}
