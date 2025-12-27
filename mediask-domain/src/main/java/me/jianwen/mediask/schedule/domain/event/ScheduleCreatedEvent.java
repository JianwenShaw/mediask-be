package me.jianwen.mediask.schedule.domain.event;

import lombok.Value;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        this.scheduleId = scheduleId;
        this.doctorId = doctorId;
        this.scheduleDate = scheduleDate;
        this.timePeriod = timePeriod;
        this.occurredOn = LocalDateTime.now();
    }
}
