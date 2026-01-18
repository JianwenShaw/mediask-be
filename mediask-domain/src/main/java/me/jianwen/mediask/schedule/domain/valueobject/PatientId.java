package me.jianwen.mediask.schedule.domain.valueobject;

import java.util.Objects;

/**
 * 患者ID值对象
 *
 * @author jianwen
 */
public record PatientId(Long value) {

    public PatientId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("患者ID必须为正数");
        }
    }

    public static PatientId of(Long value) {
        return new PatientId(value);
    }
}
