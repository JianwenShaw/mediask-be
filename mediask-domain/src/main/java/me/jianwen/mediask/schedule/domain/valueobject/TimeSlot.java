package me.jianwen.mediask.schedule.domain.valueobject;

import lombok.Value;

import java.time.LocalTime;

/**
 * 时间片值对象
 * 代表具体的就诊时段，例如 09:00-09:15
 *
 * @author jianwen
 */
@Value
public class TimeSlot {

    LocalTime startTime;
    LocalTime endTime;

    public TimeSlot(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Time cannot be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 创建时间片
     */
    public static TimeSlot of(LocalTime startTime, int durationMinutes) {
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        return new TimeSlot(startTime, endTime);
    }

    /**
     * 判断时间是否在时间片内
     */
    public boolean contains(LocalTime time) {
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    /**
     * 判断是否与另一个时间片重叠
     */
    public boolean overlaps(TimeSlot other) {
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    /**
     * 获取持续时长（分钟）
     */
    public int getDurationMinutes() {
        return (endTime.getHour() * 60 + endTime.getMinute())
                - (startTime.getHour() * 60 + startTime.getMinute());
    }

    @Override
    public String toString() {
        return startTime + "-" + endTime;
    }
}
