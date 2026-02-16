package me.jianwen.mediask.schedule.domain.service;

import me.jianwen.mediask.schedule.domain.entity.Appointment;
import me.jianwen.mediask.schedule.domain.statemachine.TransitionResult;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentEvent;
import me.jianwen.mediask.schedule.domain.valueobject.AppointmentStatus;

/**
 * 预约轻量状态机
 */
public class AppointmentStateMachineDomainService {

    public TransitionResult<AppointmentStatus, AppointmentEvent> transit(
            Appointment appointment,
            AppointmentEvent event,
            AppointmentTransitionContext context) {

        AppointmentStatus from = appointment.getStatus();

        if (isIdempotent(from, event)) {
            return new TransitionResult<>(from, from, event, false, true, "重复事件，已幂等处理");
        }

        return switch (event) {
            case PAY_SUCCESS -> doPay(appointment, from);
            case USER_CANCEL, ADMIN_CANCEL, PAY_TIMEOUT_CANCEL -> doCancel(appointment, from, event, context.cancelReason());
            case DOCTOR_MARK_VISITED -> doMarkVisited(appointment, from);
            case SYSTEM_MARK_ABSENT -> doMarkAbsent(appointment, from);
            case CREATE -> new TransitionResult<>(from, from, event, false, true, "创建事件由工厂方法处理");
        };
    }

    private TransitionResult<AppointmentStatus, AppointmentEvent> doPay(Appointment appointment, AppointmentStatus from) {
        if (from != AppointmentStatus.UNPAID) {
            throw invalidTransition(from, AppointmentEvent.PAY_SUCCESS);
        }
        appointment.markAsPaid();
        return new TransitionResult<>(from, appointment.getStatus(), AppointmentEvent.PAY_SUCCESS, true, false, null);
    }

    private TransitionResult<AppointmentStatus, AppointmentEvent> doCancel(
            Appointment appointment,
            AppointmentStatus from,
            AppointmentEvent event,
            String cancelReason) {

        if (!(from == AppointmentStatus.UNPAID || from == AppointmentStatus.CONFIRMED)) {
            throw invalidTransition(from, event);
        }
        appointment.cancel(cancelReason == null ? "取消预约" : cancelReason);
        return new TransitionResult<>(from, appointment.getStatus(), event, true, false, null);
    }

    private TransitionResult<AppointmentStatus, AppointmentEvent> doMarkVisited(
            Appointment appointment,
            AppointmentStatus from) {
        if (from != AppointmentStatus.CONFIRMED) {
            throw invalidTransition(from, AppointmentEvent.DOCTOR_MARK_VISITED);
        }
        appointment.markAsVisited();
        return new TransitionResult<>(from, appointment.getStatus(), AppointmentEvent.DOCTOR_MARK_VISITED, true, false, null);
    }

    private TransitionResult<AppointmentStatus, AppointmentEvent> doMarkAbsent(
            Appointment appointment,
            AppointmentStatus from) {
        if (from != AppointmentStatus.CONFIRMED) {
            throw invalidTransition(from, AppointmentEvent.SYSTEM_MARK_ABSENT);
        }
        appointment.markAsAbsent();
        return new TransitionResult<>(from, appointment.getStatus(), AppointmentEvent.SYSTEM_MARK_ABSENT, true, false, null);
    }

    private boolean isIdempotent(AppointmentStatus status, AppointmentEvent event) {
        if (event == AppointmentEvent.PAY_SUCCESS && status == AppointmentStatus.CONFIRMED) {
            return true;
        }
        if (status == AppointmentStatus.CANCELLED && isCancelEvent(event)) {
            return true;
        }
        if (event == AppointmentEvent.DOCTOR_MARK_VISITED && status == AppointmentStatus.VISITED) {
            return true;
        }
        return event == AppointmentEvent.SYSTEM_MARK_ABSENT && status == AppointmentStatus.ABSENT;
    }

    private boolean isCancelEvent(AppointmentEvent event) {
        return event == AppointmentEvent.USER_CANCEL
                || event == AppointmentEvent.ADMIN_CANCEL
                || event == AppointmentEvent.PAY_TIMEOUT_CANCEL;
    }

    private IllegalStateException invalidTransition(AppointmentStatus from, AppointmentEvent event) {
        return new IllegalStateException("无效状态流转: from=" + from.description() + ", event=" + event.name());
    }
}
