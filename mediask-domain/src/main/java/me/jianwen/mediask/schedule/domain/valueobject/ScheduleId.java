package me.jianwen.mediask.schedule.domain.valueobject;

import lombok.Value;

/**
 * 排班ID值对象
 *
 * @author jianwen
 */
@Value
public class ScheduleId {

    Long value;

    public static ScheduleId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Invalid schedule id");
        }
        return new ScheduleId(value);
    }

    public static ScheduleId generate() {
        // 由雪花算法生成，在应用层设置
        return null;
    }
}
