package me.jianwen.mediask.schedule.domain.valueobject;

/**
 * 预约状态流转事件
 */
public enum AppointmentEvent {

    CREATE,
    PAY_SUCCESS,
    USER_CANCEL,
    ADMIN_CANCEL,
    PAY_TIMEOUT_CANCEL,
    DOCTOR_MARK_VISITED,
    SYSTEM_MARK_ABSENT
}
