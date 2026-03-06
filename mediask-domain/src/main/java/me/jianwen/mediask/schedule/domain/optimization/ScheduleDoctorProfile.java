package me.jianwen.mediask.schedule.domain.optimization;

public record ScheduleDoctorProfile(
        Long doctorId,
        Long departmentId,
        String title,
        boolean senior
) {
}
