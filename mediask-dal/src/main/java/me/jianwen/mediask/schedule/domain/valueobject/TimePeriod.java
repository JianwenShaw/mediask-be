package me.jianwen.mediask.schedule.domain.valueobject;

import java.util.Arrays;

/**
 * 就诊时段
 */
public enum TimePeriod {
    MORNING(1),
    AFTERNOON(2),
    EVENING(3);

    private final int code;

    TimePeriod(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static TimePeriod fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("timePeriod code cannot be null");
        }
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid timePeriod code: " + code));
    }
}

