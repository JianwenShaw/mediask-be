package me.jianwen.mediask.schedule.domain.rule;

public record DoctorAvailabilityRule(
        Long doctorId,
        Integer weekday,
        Integer periodCode,
        boolean available,
        Integer priority
) {
}
