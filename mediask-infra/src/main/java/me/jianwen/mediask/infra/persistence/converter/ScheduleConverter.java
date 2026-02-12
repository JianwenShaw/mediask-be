package me.jianwen.mediask.infra.persistence.converter;

import me.jianwen.mediask.dal.entity.DoctorScheduleDO;
import me.jianwen.mediask.dal.enums.ScheduleStatusEnum;
import me.jianwen.mediask.dal.enums.TimePeriodEnum;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.valueobject.DoctorId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleId;
import me.jianwen.mediask.schedule.domain.valueobject.ScheduleStatus;
import me.jianwen.mediask.schedule.domain.valueobject.SlotCapacity;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
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
        dataObject.setPeriodStartTime(schedule.getTimePeriod().getStartTime());
        dataObject.setPeriodEndTime(schedule.getTimePeriod().getEndTime());
        dataObject.setSlotDurationMinutes(schedule.getSlotDurationMinutes());
        dataObject.setFee(schedule.getFee());
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
        schedule.setSlotDurationMinutes(
                dataObject.getSlotDurationMinutes() != null ? dataObject.getSlotDurationMinutes() : 15);
        schedule.setFee(dataObject.getFee());
        schedule.setCreatedAt(dataObject.getCreatedAt());
        schedule.setUpdatedAt(dataObject.getUpdatedAt());

        return schedule;
    }

    private ScheduleStatusEnum mapToStatusEnum(ScheduleStatus status) {
        if (status == null) {
            return ScheduleStatusEnum.OPEN;
        }
        return switch (status) {
            case OPEN -> ScheduleStatusEnum.OPEN;
            case CLOSED -> ScheduleStatusEnum.CLOSED;
            case FULL -> ScheduleStatusEnum.FULL;
            case EXPIRED -> ScheduleStatusEnum.EXPIRED;
        };
    }

    private ScheduleStatus mapToScheduleStatus(ScheduleStatusEnum statusEnum) {
        if (statusEnum == null) {
            return ScheduleStatus.OPEN;
        }
        return switch (statusEnum) {
            case OPEN -> ScheduleStatus.OPEN;
            case CLOSED -> ScheduleStatus.CLOSED;
            case FULL -> ScheduleStatus.FULL;
            case EXPIRED -> ScheduleStatus.EXPIRED;
        };
    }
}
