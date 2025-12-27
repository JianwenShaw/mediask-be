package me.jianwen.mediask.schedule.domain.event;

import lombok.Value;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;

import java.time.LocalDateTime;

/**
 * 号源增加事件
 *
 * @author jianwen
 */
@Value
public class ScheduleSlotIncreasedEvent {

    ScheduleId scheduleId;
    int remainingSlots;
    LocalDateTime occurredOn;

    public ScheduleSlotIncreasedEvent(ScheduleId scheduleId, int remainingSlots) {
        this.scheduleId = scheduleId;
        this.remainingSlots = remainingSlots;
        this.occurredOn = LocalDateTime.now();
    }
}
