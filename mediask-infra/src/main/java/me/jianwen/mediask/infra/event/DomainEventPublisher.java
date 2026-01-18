package me.jianwen.mediask.infra.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotDecreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotIncreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器
 *
 * 职责：
 * 1. 将领域事件转换为Spring事件并发布
 * 2. 统一事件发布入口
 *
 * @author jianwen
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DomainEventPublisher implements me.jianwen.mediask.domain.event.DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布预约创建事件
     */
    @Override
    public void publishAppointmentCreated(AppointmentCreatedEvent event) {
        log.debug("发布预约创建事件: appointmentId={}", event.appointmentId().value());
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布预约状态变更事件
     */
    @Override
    public void publishAppointmentStatusChanged(AppointmentStatusChangedEvent event) {
        log.debug("发布预约状态变更事件: appointmentId={}, oldStatus={}, newStatus={}",
                event.appointmentId().value(),
                event.oldStatus().description(),
                event.newStatus().description());
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班创建事件
     */
    @Override
    public void publishScheduleCreated(ScheduleCreatedEvent event) {
        log.debug("发布排班创建事件");
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班号源扣减事件
     */
    @Override
    public void publishScheduleSlotDecreased(ScheduleSlotDecreasedEvent event) {
        log.debug("发布排班号源扣减事件");
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班号源增加事件
     */
    @Override
    public void publishScheduleSlotIncreased(ScheduleSlotIncreasedEvent event) {
        log.debug("发布排班号源增加事件");
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班状态变更事件
     */
    @Override
    public void publishScheduleStatusChanged(ScheduleStatusChangedEvent event) {
        log.debug("发布排班状态变更事件: oldStatus={}, newStatus={}",
                event.getOldStatus().getDescription(),
                event.getNewStatus().getDescription());
        applicationEventPublisher.publishEvent(event);
    }
}
