package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.schedule.AutoScheduleRequest;
import me.jianwen.mediask.api.model.schedule.CreateScheduleRequest;
import me.jianwen.mediask.api.model.schedule.ScheduleResponse;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(config = MapStructConfig.class)
public interface ScheduleApiMapper {

    me.jianwen.mediask.service.application.command.CreateScheduleCommand toService(CreateScheduleRequest request);

    me.jianwen.mediask.service.application.command.AutoScheduleCommand toService(AutoScheduleRequest request);

    @Mappings({
            @Mapping(target = "scheduleId", expression = "java(schedule.getId() != null ? schedule.getId().getValue() : null)"),
            @Mapping(target = "doctorId", expression = "java(schedule.getDoctorId() != null ? schedule.getDoctorId().getValue() : null)"),
            @Mapping(target = "timePeriodCode", expression = "java(schedule.getTimePeriod() != null ? schedule.getTimePeriod().getCode() : null)"),
            @Mapping(target = "timePeriodDesc", expression = "java(schedule.getTimePeriod() != null ? schedule.getTimePeriod().getDescription() : null)"),
            @Mapping(target = "totalSlots", expression = "java(schedule.getCapacity() != null ? schedule.getCapacity().getTotalSlots() : null)"),
            @Mapping(target = "availableSlots", expression = "java(schedule.getCapacity() != null ? schedule.getCapacity().getAvailableSlots() : null)"),
            @Mapping(target = "statusCode", expression = "java(schedule.getStatus() != null ? schedule.getStatus().getCode() : null)"),
            @Mapping(target = "statusDesc", expression = "java(schedule.getStatus() != null ? schedule.getStatus().getDescription() : null)")
    })
    ScheduleResponse toResponse(DoctorSchedule schedule);
}
