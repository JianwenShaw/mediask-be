package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.request.schedule.CreateScheduleRuleProfileRequest;
import me.jianwen.mediask.api.security.CurrentUserProvider;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.command.CreateScheduleRuleProfileCommand;
import me.jianwen.mediask.service.application.dto.schedule.ScheduleRuleProfileDTO;
import me.jianwen.mediask.service.application.dto.schedule.ScheduleRuleProfilePublishResultDTO;
import me.jianwen.mediask.service.application.dto.schedule.ScheduleRuleProfileVersionDTO;
import me.jianwen.mediask.service.application.ScheduleRuleProfileApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排班规则配置管理。
 */
@RestController
@RequestMapping("/api/v1/schedule-rule-profiles")
@Tag(name = "排班规则配置", description = "JSON DSL 规则配置的创建、发布与回滚")
@RequiredArgsConstructor
public class ScheduleRuleProfileController {

    private final ScheduleRuleProfileApplicationService scheduleRuleProfileApplicationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "创建规则配置草稿版本")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<ScheduleRuleProfileDTO> createDraft(@Validated @RequestBody CreateScheduleRuleProfileRequest request) {
        CreateScheduleRuleProfileCommand command = new CreateScheduleRuleProfileCommand();
        command.setDepartmentId(request.getDepartmentId());
        command.setProfileCode(request.getProfileCode());
        command.setProfileName(request.getProfileName());
        command.setConstraintDslJson(request.getConstraintDslJson());
        command.setDescription(request.getDescription());
        command.setOperatorId(currentUserProvider.currentUserId());
        return Result.ok(scheduleRuleProfileApplicationService.createDraft(command));
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "查询规则配置详情")
    @PreAuthorize("hasAuthority('schedule:query')")
    public Result<ScheduleRuleProfileDTO> getProfile(
            @Parameter(description = "规则配置ID") @PathVariable Long profileId) {
        return Result.ok(scheduleRuleProfileApplicationService.getProfile(profileId));
    }

    @GetMapping("/versions")
    @Operation(summary = "查询规则配置版本列表")
    @PreAuthorize("hasAuthority('schedule:query')")
    public Result<List<ScheduleRuleProfileVersionDTO>> listVersions(
            @Parameter(description = "科室ID") @RequestParam Long departmentId,
            @Parameter(description = "规则编码") @RequestParam String profileCode) {
        return Result.ok(scheduleRuleProfileApplicationService.listVersions(departmentId, profileCode));
    }

    @PostMapping("/{profileId}/publish")
    @Operation(summary = "发布规则配置")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<ScheduleRuleProfilePublishResultDTO> publish(
            @Parameter(description = "规则配置ID") @PathVariable Long profileId) {
        return Result.ok(scheduleRuleProfileApplicationService.publish(profileId));
    }

    @PostMapping("/{profileId}/rollback")
    @Operation(summary = "回滚发布到指定规则版本")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<ScheduleRuleProfilePublishResultDTO> rollback(
            @Parameter(description = "规则配置ID") @PathVariable Long profileId) {
        return Result.ok(scheduleRuleProfileApplicationService.rollback(profileId));
    }
}
