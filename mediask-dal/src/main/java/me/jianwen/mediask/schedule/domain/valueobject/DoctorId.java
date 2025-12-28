package me.jianwen.mediask.schedule.domain.valueobject;

import java.util.Objects;

/**
 * 医生ID 值对象
 */
public final class DoctorId {

    private final Long value;

    private DoctorId(Long value) {
        this.value = value;
    }

    public static DoctorId of(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("doctorId cannot be null");
        }
        return new DoctorId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoctorId doctorId)) return false;
        return Objects.equals(value, doctorId.value);
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

