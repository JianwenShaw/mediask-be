package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.mapper.ScheduleTemplateApiMapper;
import me.jianwen.mediask.api.model.schedule.CreateScheduleTemplateRequest;
import me.jianwen.mediask.api.model.schedule.ScheduleTemplateResponse;
import me.jianwen.mediask.api.model.schedule.UpdateScheduleTemplateRequest;
import me.jianwen.mediask.common.dto.schedule.ScheduleTemplateDTO;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.service.ScheduleTemplateApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedule-templates")
@Tag(name = "排班模板", description = "排班模板管理接口")
@RequiredArgsConstructor
public class ScheduleTemplateController {

    private final ScheduleTemplateApplicationService scheduleTemplateApplicationService;
    private final ScheduleTemplateApiMapper scheduleTemplateApiMapper;

    @PostMapping
    @Operation(summary = "创建排班模板")
    @PreAuthorize("hasAuthority('schedule:create')")
    public Result<Long> createTemplate(@Validated @RequestBody CreateScheduleTemplateRequest request) {
        var serviceRequest = scheduleTemplateApiMapper.toService(request);
        return Result.ok(scheduleTemplateApplicationService.createTemplate(serviceRequest));
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "更新排班模板")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<Void> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Validated @RequestBody UpdateScheduleTemplateRequest request) {
        var serviceRequest = scheduleTemplateApiMapper.toService(request);
        scheduleTemplateApplicationService.updateTemplate(templateId, serviceRequest);
        return Result.ok();
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "查询排班模板详情")
    @PreAuthorize("hasAuthority('schedule:query')")
    public Result<ScheduleTemplateResponse> getTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        ScheduleTemplateDTO template = scheduleTemplateApplicationService.getTemplate(templateId);
        return Result.ok(scheduleTemplateApiMapper.toResponse(template));
    }

    @PostMapping("/{templateId}/publish")
    @Operation(summary = "发布排班模板")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<Void> publishTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        scheduleTemplateApplicationService.publishTemplate(templateId);
        return Result.ok();
    }
}
