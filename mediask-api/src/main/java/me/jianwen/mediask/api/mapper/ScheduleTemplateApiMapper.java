package me.jianwen.mediask.api.mapper;

import me.jianwen.mediask.api.model.schedule.CreateScheduleTemplateRequest;
import me.jianwen.mediask.api.model.schedule.GenerateScheduleFromTemplateRequest;
import me.jianwen.mediask.api.model.schedule.ScheduleTemplateRuleResponse;
import me.jianwen.mediask.api.model.schedule.ScheduleTemplateResponse;
import me.jianwen.mediask.api.model.schedule.UpdateScheduleTemplateRequest;
import me.jianwen.mediask.common.dto.schedule.ScheduleTemplateDTO;
import me.jianwen.mediask.common.dto.schedule.ScheduleTemplateRuleDTO;
import me.jianwen.mediask.service.application.command.CreateScheduleTemplateCommand;
import me.jianwen.mediask.service.application.command.GenerateScheduleFromTemplateCommand;
import me.jianwen.mediask.service.application.command.UpdateScheduleTemplateCommand;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public interface ScheduleTemplateApiMapper {

    CreateScheduleTemplateCommand toService(CreateScheduleTemplateRequest request);

    UpdateScheduleTemplateCommand toService(UpdateScheduleTemplateRequest request);

    GenerateScheduleFromTemplateCommand toService(GenerateScheduleFromTemplateRequest request);

    ScheduleTemplateResponse toResponse(ScheduleTemplateDTO dto);

    ScheduleTemplateRuleResponse toResponse(ScheduleTemplateRuleDTO dto);
}
