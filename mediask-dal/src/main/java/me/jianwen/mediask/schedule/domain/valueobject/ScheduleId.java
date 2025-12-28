package me.jianwen.mediask.schedule.domain.valueobject;

import java.util.Objects;

/**
 * 排班ID 值对象
 */
public final class ScheduleId {

    private final Long value;

    private ScheduleId(Long value) {
        this.value = value;
    }

    public static ScheduleId of(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("scheduleId cannot be null");
        }
        return new ScheduleId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScheduleId scheduleId)) return false;
        return Objects.equals(value, scheduleId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

