package me.jianwen.mediask.schedule.domain.event;

import lombok.Value;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleStatus;

import java.time.LocalDateTime;

/**
 * 排班状态变更事件
 *
 * @author jianwen
 */
@Value
public class ScheduleStatusChangedEvent {

    ScheduleId scheduleId;
    ScheduleStatus oldStatus;
    ScheduleStatus newStatus;
    LocalDateTime occurredOn;

    public ScheduleStatusChangedEvent(
            ScheduleId scheduleId,
            ScheduleStatus oldStatus,
            ScheduleStatus newStatus) {
        this.scheduleId = scheduleId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.occurredOn = LocalDateTime.now();
    }
}
