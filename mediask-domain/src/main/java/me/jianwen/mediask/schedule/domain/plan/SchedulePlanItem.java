package me.jianwen.mediask.schedule.domain.plan;

import java.time.LocalDate;

public record SchedulePlanItem(
        LocalDate scheduleDate,
        Integer periodCode,
        Long doctorId,
        boolean senior,
        String reasonJson,
        String penaltyJson,
        Double scoreDelta
) {
}
