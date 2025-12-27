package me.jianwen.mediask.schedule.domain.event;

import lombok.Value;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;

import java.time.LocalDateTime;

/**
 * 号源扣减事件
 *
 * @author jianwen
 */
@Value
public class ScheduleSlotDecreasedEvent {

    ScheduleId scheduleId;
    int remainingSlots;
    LocalDateTime occurredOn;

    public ScheduleSlotDecreasedEvent(ScheduleId scheduleId, int remainingSlots) {
        this.scheduleId = scheduleId;
        this.remainingSlots = remainingSlots;
        this.occurredOn = LocalDateTime.now();
    }
}
