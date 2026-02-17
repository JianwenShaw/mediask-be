package me.jianwen.mediask.schedule.domain.optimization.model;

public record DoctorAvailabilityRule(
        Long doctorId,
        Integer weekday,
        Integer periodCode,
        boolean available,
        Integer priority
) {
}
