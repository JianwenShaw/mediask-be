package me.jianwen.mediask.schedule.domain.event;

import lombok.Value;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 排班创建事件
 *
 * @author jianwen
 */
@Value
public class ScheduleCreatedEvent {

    ScheduleId scheduleId;
    DoctorId doctorId;
    LocalDate scheduleDate;
    TimePeriod timePeriod;
    LocalDateTime occurredOn;

    public ScheduleCreatedEvent(
            ScheduleId scheduleId,
            DoctorId doctorId,
            LocalDate scheduleDate,
            TimePeriod timePeriod) {
        this.scheduleId = Objects.requireNonNull(scheduleId, "scheduleId不能为空");
        this.doctorId = Objects.requireNonNull(doctorId, "doctorId不能为空");
        this.scheduleDate = Objects.requireNonNull(scheduleDate, "scheduleDate不能为空");
        this.timePeriod = Objects.requireNonNull(timePeriod, "timePeriod不能为空");
        this.occurredOn = LocalDateTime.now();
    }
}
