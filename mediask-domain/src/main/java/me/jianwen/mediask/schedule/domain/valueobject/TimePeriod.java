package me.jianwen.mediask.schedule.domain.valueobject;

import lombok.Getter;
import lombok.Value;

import java.time.LocalTime;
import java.util.Arrays;

/**
 * 时段枚举值对象
 *
 * @author jianwen
 */
@Getter
public enum TimePeriod {

    MORNING(1, "上午", LocalTime.of(8, 0), LocalTime.of(12, 0)),
    AFTERNOON(2, "下午", LocalTime.of(14, 0), LocalTime.of(18, 0)),
    EVENING(3, "晚上", LocalTime.of(19, 0), LocalTime.of(21, 0));

    private final Integer code;
    private final String description;
    private final LocalTime startTime;
    private final LocalTime endTime;

    TimePeriod(Integer code, String description, LocalTime startTime, LocalTime endTime) {
        this.code = code;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 从代码创建
     */
    public static TimePeriod fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid time period code: " + code));
    }

    /**
     * 判断时间是否在当前时段内
     */
    public boolean isInPeriod(LocalTime time) {
        return !time.isBefore(startTime) && !time.isAfter(endTime);
    }

    /**
     * 计算时段内可以划分多少个时间片
     * 
     * @param slotMinutes 每个时间片的分钟数
     */
    public int calculateSlotsCount(int slotMinutes) {
        int totalMinutes = (endTime.getHour() * 60 + endTime.getMinute())
                - (startTime.getHour() * 60 + startTime.getMinute());
        return totalMinutes / slotMinutes;
    }
}
