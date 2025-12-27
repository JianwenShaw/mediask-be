package me.jianwen.mediask.schedule.domain.valueobject;

import lombok.Value;

/**
 * 医生ID值对象
 *
 * @author jianwen
 */
@Value
public class DoctorId {

    Long value;

    public static DoctorId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Invalid doctor id");
        }
        return new DoctorId(value);
    }
}
