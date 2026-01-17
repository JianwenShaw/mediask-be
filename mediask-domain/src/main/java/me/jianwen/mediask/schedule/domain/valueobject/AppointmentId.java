package me.jianwen.mediask.schedule.domain.valueobject;

import java.util.Objects;

/**
 * 预约ID值对象
 *
 * @author jianwen
 */
public record AppointmentId(Long value) {

    public AppointmentId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("预约ID必须为正数");
        }
    }

    public static AppointmentId of(Long value) {
        return new AppointmentId(value);
    }
}
