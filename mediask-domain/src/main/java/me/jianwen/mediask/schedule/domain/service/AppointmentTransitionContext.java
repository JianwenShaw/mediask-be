package me.jianwen.mediask.schedule.domain.service;

import me.jianwen.mediask.schedule.domain.entity.Appointment;

/**
 * 预约状态转移上下文
 */
public record AppointmentTransitionContext(
        Appointment appointment,
        String cancelReason
) {

    public static AppointmentTransitionContext of(Appointment appointment) {
        return new AppointmentTransitionContext(appointment, null);
    }

    public static AppointmentTransitionContext forCancel(Appointment appointment, String cancelReason) {
        return new AppointmentTransitionContext(appointment, cancelReason);
    }
}
