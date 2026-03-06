package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.mapper.config.MapStructConfig;
import me.jianwen.mediask.api.request.schedule.CreateScheduleRequest;
import me.jianwen.mediask.api.response.schedule.ScheduleResponse;
import me.jianwen.mediask.service.application.command.CreateScheduleCommand;
import me.jianwen.mediask.service.application.dto.schedule.ScheduleDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface ScheduleApiMapper {

    CreateScheduleCommand toService(CreateScheduleRequest request);

    ScheduleResponse toResponse(ScheduleDTO schedule);
}
