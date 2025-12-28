package me.jianwen.mediask.schedule.domain.valueobject;

import java.util.Arrays;

/**
 * 排班状态
 */
public enum ScheduleStatus {
    OPEN(1),
    CLOSED(2),
    EXPIRED(3);

    private final int code;

    ScheduleStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ScheduleStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("scheduleStatus code cannot be null");
        }
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid scheduleStatus code: " + code));
    }
}

