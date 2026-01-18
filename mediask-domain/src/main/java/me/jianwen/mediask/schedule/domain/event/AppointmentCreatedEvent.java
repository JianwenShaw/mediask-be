package me.jianwen.mediask.schedule.domain.event;

import me.jianwen.mediask.schedule.domain.valueobject.AppointmentId;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.PatientId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约创建事件
 *
 * @author jianwen
 */
public record AppointmentCreatedEvent(
        AppointmentId appointmentId,
        PatientId patientId,
        DoctorId doctorId,
        LocalDate apptDate,
        String apptNo,
        BigDecimal apptFee,
        LocalDateTime createdAt
) {
}
