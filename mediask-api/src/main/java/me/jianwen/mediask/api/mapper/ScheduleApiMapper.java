package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.schedule.AutoScheduleRequest;
import me.jianwen.mediask.api.model.schedule.CreateScheduleRequest;
import me.jianwen.mediask.api.model.schedule.ScheduleResponse;
import me.jianwen.mediask.common.dto.schedule.ScheduleDTO;
import me.jianwen.mediask.service.application.command.AutoScheduleCommand;
import me.jianwen.mediask.service.application.command.CreateScheduleCommand;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface ScheduleApiMapper {

    CreateScheduleCommand toService(CreateScheduleRequest request);

    AutoScheduleCommand toService(AutoScheduleRequest request);

    ScheduleResponse toResponse(ScheduleDTO schedule);
}
