package me.jianwen.mediask.schedule.domain.event;

import me.jianwen.mediask.schedule.domain.valueobject.AppointmentId;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentStatus;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.PatientId;

import java.time.LocalDateTime;

/**
 * 预约状态变更事件
 *
 * @author jianwen
 */
public record AppointmentStatusChangedEvent(
        AppointmentId appointmentId,
        PatientId patientId,
        DoctorId doctorId,
        AppointmentStatus oldStatus,
        AppointmentStatus newStatus,
        LocalDateTime changedAt
) {
}
