package me.jianwen.mediask.schedule.domain.optimization.model;

public record ScheduleDoctorProfile(
        Long doctorId,
        Long departmentId,
        String title,
        boolean senior
) {
}
