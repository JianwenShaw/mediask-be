package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.mapper.config.MapStructConfig;
import me.jianwen.mediask.api.request.schedule.CreateScheduleTemplateRequest;
import me.jianwen.mediask.api.request.schedule.GenerateScheduleFromTemplateRequest;
import me.jianwen.mediask.api.request.schedule.UpdateScheduleTemplateRequest;
import me.jianwen.mediask.api.response.schedule.ScheduleTemplateResponse;
import me.jianwen.mediask.api.response.schedule.ScheduleTemplateRuleResponse;
import me.jianwen.mediask.service.application.command.CreateScheduleTemplateCommand;
import me.jianwen.mediask.service.application.command.GenerateScheduleFromTemplateCommand;
import me.jianwen.mediask.service.application.command.UpdateScheduleTemplateCommand;
import me.jianwen.mediask.service.application.dto.schedule.ScheduleTemplateDTO;
import me.jianwen.mediask.service.application.dto.schedule.ScheduleTemplateRuleDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface ScheduleTemplateApiMapper {

    CreateScheduleTemplateCommand toService(CreateScheduleTemplateRequest request);

    UpdateScheduleTemplateCommand toService(UpdateScheduleTemplateRequest request);

    GenerateScheduleFromTemplateCommand toService(GenerateScheduleFromTemplateRequest request);

    ScheduleTemplateResponse toResponse(ScheduleTemplateDTO dto);

    ScheduleTemplateRuleResponse toResponse(ScheduleTemplateRuleDTO dto);
}
