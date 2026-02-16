package me.jianwen.mediask.schedule.domain.service;

import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.statemachine.TransitionResult;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentEvent;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentStatus;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.PatientId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentStateMachineDomainServiceTest {

    private final AppointmentStateMachineDomainService service = new AppointmentStateMachineDomainService();

    @Test
    void shouldTransitToConfirmedWhenPaySuccessOnUnpaid() {
        Appointment appointment = createUnpaidAppointment();

        TransitionResult<AppointmentStatus, AppointmentEvent> result = service.transit(
                appointment,
                AppointmentEvent.PAY_SUCCESS,
                AppointmentTransitionContext.of(appointment)
        );

        assertTrue(result.stateChanged());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
    }

    @Test
    void shouldIdempotentWhenPaySuccessOnConfirmed() {
        Appointment appointment = createUnpaidAppointment();
        appointment.markAsPaid();

        TransitionResult<AppointmentStatus, AppointmentEvent> result = service.transit(
                appointment,
                AppointmentEvent.PAY_SUCCESS,
                AppointmentTransitionContext.of(appointment)
        );

        assertFalse(result.stateChanged());
        assertTrue(result.idempotent());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
    }

    @Test
    void shouldThrowWhenMarkVisitedFromUnpaid() {
        Appointment appointment = createUnpaidAppointment();

        assertThrows(IllegalStateException.class, () -> service.transit(
                appointment,
                AppointmentEvent.DOCTOR_MARK_VISITED,
                AppointmentTransitionContext.of(appointment)
        ));
    }

    private Appointment createUnpaidAppointment() {
        return Appointment.create(
                PatientId.of(1L),
                DoctorId.of(2L),
                ScheduleId.of(3L),
                LocalDate.now().plusDays(1),
                TimePeriod.MORNING,
                LocalTime.of(9, 0),
                BigDecimal.TEN,
                "主诉",
                "APPT-TEST"
        );
    }
}
