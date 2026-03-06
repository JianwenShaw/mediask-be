package me.jianwen.mediask.schedule.domain.port;

import me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotDecreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotIncreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleStatusChangedEvent;

/**
 * 领域事件发布器（应用层依赖的端口）
 */
public interface ScheduleDomainEventPublisher {

    void publishAppointmentCreated(AppointmentCreatedEvent event);

    void publishAppointmentStatusChanged(AppointmentStatusChangedEvent event);

    void publishScheduleCreated(ScheduleCreatedEvent event);

    void publishScheduleSlotDecreased(ScheduleSlotDecreasedEvent event);

    void publishScheduleSlotIncreased(ScheduleSlotIncreasedEvent event);

    void publishScheduleStatusChanged(ScheduleStatusChangedEvent event);
}
