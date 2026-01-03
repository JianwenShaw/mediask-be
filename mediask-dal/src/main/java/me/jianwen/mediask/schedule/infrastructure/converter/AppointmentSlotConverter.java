package me.jianwen.mediask.schedule.infrastructure.converter;

import me.jianwen.mediask.dal.entity.AppointmentSlotDO;
import me.jianwen.mediask.schedule.domain.entity.AppointmentSlot;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.TimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.time.LocalTime;

/**
 * 号源时段对象转换器
 *
 * <p>
 * 当前表结构仅保存 {@code slot_time}（开始时间），结束时间先按默认时长（15 分钟）推导。
 * 后续如果表结构补充了结束时间或时长字段，再将该推导逻辑替换为精确映射。
 *
 * @author jianwen
 */
@Mapper(componentModel = "spring")
public interface AppointmentSlotConverter {

    AppointmentSlotConverter INSTANCE = Mappers.getMapper(AppointmentSlotConverter.class);

    /**
     * 领域对象 -> 数据对象
     */
    default AppointmentSlotDO toDataObject(AppointmentSlot slot) {
        if (slot == null) {
            return null;
        }

        AppointmentSlotDO dataObject = new AppointmentSlotDO();
        dataObject.setId(slot.getId());
        if (slot.getScheduleId() != null) {
            dataObject.setScheduleId(slot.getScheduleId().getValue());
        }
        dataObject.setSlotTime(extractStartTime(slot));
        dataObject.setIsOccupied(slot.isOccupied() ? 1 : 0);
        dataObject.setApptId(slot.getAppointmentId());
        dataObject.setCreatedAt(slot.getCreatedAt());
        dataObject.setUpdatedAt(slot.getUpdatedAt());
        return dataObject;
    }

    /**
     * 数据对象 -> 领域对象
     */
    default AppointmentSlot toDomain(AppointmentSlotDO dataObject) {
        if (dataObject == null) {
            return null;
        }

        AppointmentSlot slot = new AppointmentSlot();
        slot.setId(dataObject.getId());
        slot.setScheduleId(ScheduleId.of(dataObject.getScheduleId()));
        slot.setTimeSlot(TimeSlot.of(dataObject.getSlotTime(), 15));
        slot.setOccupied(dataObject.getIsOccupied() != null && dataObject.getIsOccupied() == 1);
        slot.setAppointmentId(dataObject.getApptId());
        slot.setCreatedAt(dataObject.getCreatedAt());
        slot.setUpdatedAt(dataObject.getUpdatedAt());
        return slot;
    }

    private LocalTime extractStartTime(AppointmentSlot slot) {
        TimeSlot timeSlot = slot.getTimeSlot();
        if (timeSlot == null) {
            return null;
        }
        return timeSlot.getStartTime();
    }
}


