package me.jianwen.mediask.schedule.infrastructure.converter;

import me.jianwen.mediask.dal.entity.DoctorScheduleDO;
import me.jianwen.mediask.dal.enums.StatusEnum;
import me.jianwen.mediask.dal.enums.TimePeriodEnum;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.valueobject.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 排班对象转换器
 *
 * @author jianwen
 */
@Mapper(componentModel = "spring")
public interface ScheduleConverter {

    ScheduleConverter INSTANCE = Mappers.getMapper(ScheduleConverter.class);

    /**
     * 领域对象 -> 数据对象
     */
    default DoctorScheduleDO toDataObject(DoctorSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        DoctorScheduleDO dataObject = new DoctorScheduleDO();
        if (schedule.getId() != null) {
            dataObject.setId(schedule.getId().getValue());
        }
        dataObject.setDoctorId(schedule.getDoctorId().getValue());
        dataObject.setScheduleDate(schedule.getScheduleDate());
        dataObject.setTimePeriod(TimePeriodEnum.fromCode(schedule.getTimePeriod().getCode()));
        dataObject.setTotalSlots(schedule.getCapacity().getTotalSlots());
        dataObject.setAvailableSlots(schedule.getCapacity().getAvailableSlots());
        dataObject.setStatus(mapToStatusEnum(schedule.getStatus()));
        dataObject.setCreatedAt(schedule.getCreatedAt());
        dataObject.setUpdatedAt(schedule.getUpdatedAt());

        return dataObject;
    }

    /**
     * 数据对象 -> 领域对象
     */
    default DoctorSchedule toDomain(DoctorScheduleDO dataObject) {
        if (dataObject == null) {
            return null;
        }

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(ScheduleId.of(dataObject.getId()));
        schedule.setDoctorId(DoctorId.of(dataObject.getDoctorId()));
        schedule.setScheduleDate(dataObject.getScheduleDate());
        schedule.setTimePeriod(TimePeriod.fromCode(dataObject.getTimePeriod().getCode()));
        schedule.setCapacity(new SlotCapacity(
                dataObject.getTotalSlots(),
                dataObject.getAvailableSlots()));
        schedule.setStatus(mapToScheduleStatus(dataObject.getStatus()));
        schedule.setSlotDurationMinutes(15); // TODO: 从数据库读取
        schedule.setCreatedAt(dataObject.getCreatedAt());
        schedule.setUpdatedAt(dataObject.getUpdatedAt());

        return schedule;
    }

    private StatusEnum mapToStatusEnum(ScheduleStatus status) {
        if (status == null) {
            return StatusEnum.ENABLED;
        }
        return status == ScheduleStatus.OPEN ? StatusEnum.ENABLED : StatusEnum.DISABLED;
    }

    private ScheduleStatus mapToScheduleStatus(StatusEnum statusEnum) {
        if (statusEnum == null) {
            return ScheduleStatus.OPEN;
        }
        return statusEnum == StatusEnum.ENABLED ? ScheduleStatus.OPEN : ScheduleStatus.CLOSED;
    }
}
