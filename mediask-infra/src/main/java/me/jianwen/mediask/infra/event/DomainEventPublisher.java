package me.jianwen.mediask.infra.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.dal.entity.AppointmentEventDO;
import me.jianwen.mediask.dal.entity.ScheduleEventDO;
import me.jianwen.mediask.dal.mapper.AppointmentEventMapper;
import me.jianwen.mediask.dal.mapper.ScheduleEventMapper;
import me.jianwen.mediask.schedule.domain.event.AppointmentCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.AppointmentStatusChangedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleCreatedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotDecreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleSlotIncreasedEvent;
import me.jianwen.mediask.schedule.domain.event.ScheduleStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

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
    private final AppointmentEventMapper appointmentEventMapper;
    private final ScheduleEventMapper scheduleEventMapper;

    /**
     * 发布预约创建事件
     */
    @Override
    public void publishAppointmentCreated(AppointmentCreatedEvent event) {
        AppointmentEventDO eventDO = new AppointmentEventDO();
        eventDO.setAppointmentId(event.appointmentId() != null ? event.appointmentId().value() : null);
        eventDO.setEventType("APPOINTMENT_CREATED");
        eventDO.setToStatus(1);
        eventDO.setOperatorType("SYSTEM");
        eventDO.setOccurredAt(event.createdAt());
        eventDO.setPayloadJson("{\"apptNo\":\"" + event.apptNo() + "\"}");
        appointmentEventMapper.insert(eventDO);

        log.debug("发布预约创建事件: appointmentId={}",
                event.appointmentId() != null ? event.appointmentId().value() : null);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布预约状态变更事件
     */
    @Override
    public void publishAppointmentStatusChanged(AppointmentStatusChangedEvent event) {
        AppointmentEventDO eventDO = new AppointmentEventDO();
        eventDO.setAppointmentId(event.appointmentId() != null ? event.appointmentId().value() : null);
        eventDO.setEventType("APPOINTMENT_STATUS_CHANGED");
        eventDO.setFromStatus(event.oldStatus() != null ? event.oldStatus().code() : null);
        eventDO.setToStatus(event.newStatus() != null ? event.newStatus().code() : null);
        eventDO.setOperatorType("SYSTEM");
        eventDO.setOccurredAt(event.changedAt());
        appointmentEventMapper.insert(eventDO);

        log.debug("发布预约状态变更事件: appointmentId={}, oldStatus={}, newStatus={}",
                event.appointmentId() != null ? event.appointmentId().value() : null,
                event.oldStatus().description(),
                event.newStatus().description());
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班创建事件
     */
    @Override
    public void publishScheduleCreated(ScheduleCreatedEvent event) {
        ScheduleEventDO eventDO = new ScheduleEventDO();
        eventDO.setScheduleId(Objects.requireNonNull(event.getScheduleId(), "排班创建事件缺少scheduleId").getValue());
        eventDO.setEventType("SCHEDULE_CREATED");
        eventDO.setToStatus(1);
        eventDO.setOccurredAt(event.getOccurredOn());
        scheduleEventMapper.insert(eventDO);

        log.debug("发布排班创建事件");
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班号源扣减事件
     */
    @Override
    public void publishScheduleSlotDecreased(ScheduleSlotDecreasedEvent event) {
        ScheduleEventDO eventDO = new ScheduleEventDO();
        eventDO.setScheduleId(event.getScheduleId() != null ? event.getScheduleId().getValue() : null);
        eventDO.setEventType("SCHEDULE_SLOT_DECREASED");
        eventDO.setOccurredAt(event.getOccurredOn());
        scheduleEventMapper.insert(eventDO);

        log.debug("发布排班号源扣减事件");
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班号源增加事件
     */
    @Override
    public void publishScheduleSlotIncreased(ScheduleSlotIncreasedEvent event) {
        ScheduleEventDO eventDO = new ScheduleEventDO();
        eventDO.setScheduleId(event.getScheduleId() != null ? event.getScheduleId().getValue() : null);
        eventDO.setEventType("SCHEDULE_SLOT_INCREASED");
        eventDO.setOccurredAt(event.getOccurredOn());
        scheduleEventMapper.insert(eventDO);

        log.debug("发布排班号源增加事件");
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布排班状态变更事件
     */
    @Override
    public void publishScheduleStatusChanged(ScheduleStatusChangedEvent event) {
        ScheduleEventDO eventDO = new ScheduleEventDO();
        eventDO.setScheduleId(event.getScheduleId() != null ? event.getScheduleId().getValue() : null);
        eventDO.setEventType("SCHEDULE_STATUS_CHANGED");
        eventDO.setFromStatus(event.getOldStatus() != null ? event.getOldStatus().getCode() : null);
        eventDO.setToStatus(event.getNewStatus() != null ? event.getNewStatus().getCode() : null);
        eventDO.setOccurredAt(event.getOccurredOn());
        scheduleEventMapper.insert(eventDO);

        log.debug("发布排班状态变更事件: oldStatus={}, newStatus={}",
                event.getOldStatus().getDescription(),
                event.getNewStatus().getDescription());
        applicationEventPublisher.publishEvent(event);
    }
}
