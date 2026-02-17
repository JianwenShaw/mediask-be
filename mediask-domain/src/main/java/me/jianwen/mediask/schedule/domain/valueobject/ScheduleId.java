package me.jianwen.mediask.schedule.domain.valueobject;

import lombok.Value;
import me.jianwen.mediask.common.util.SnowflakeIdWorker;

/**
 * 排班ID值对象
 *
 * @author jianwen
 */
@Value
public class ScheduleId {

    private static final SnowflakeIdWorker ID_WORKER = new SnowflakeIdWorker(1, 1);

    Long value;

    public static ScheduleId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Invalid schedule id");
        }
        return new ScheduleId(value);
    }

    public static ScheduleId generate() {
        return new ScheduleId(ID_WORKER.nextId());
    }
}
