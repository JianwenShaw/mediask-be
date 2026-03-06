package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.dto.schedule.SchedulePlanPrecheckResultDTO;
import me.jianwen.mediask.service.application.dto.schedule.SchedulePlanPublishResultDTO;
import me.jianwen.mediask.service.application.dto.schedule.SchedulePlanVersionDTO;
import me.jianwen.mediask.service.application.SchedulePlanApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule-plans")
@Tag(name = "排班方案管理", description = "排班方案版本、发布与回滚接口")
@RequiredArgsConstructor
public class SchedulePlanController {

    private final SchedulePlanApplicationService schedulePlanApplicationService;

    @GetMapping("/{planCode}/versions")
    @Operation(summary = "查询排班方案版本列表")
    @PreAuthorize("hasAuthority('schedule:query')")
    public Result<List<SchedulePlanVersionDTO>> listVersions(
            @Parameter(description = "方案编码") @PathVariable String planCode) {
        return Result.ok(schedulePlanApplicationService.listVersions(planCode));
    }

    @PostMapping("/{planId}/publish")
    @Operation(summary = "发布排班方案")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<SchedulePlanPublishResultDTO> publish(
            @Parameter(description = "方案ID") @PathVariable Long planId,
            @Parameter(description = "发布模式：STRICT/FORCE") @RequestParam(defaultValue = "STRICT") String mode) {
        return Result.ok(schedulePlanApplicationService.publish(planId, mode));
    }

    @GetMapping("/{planId}/precheck")
    @Operation(summary = "发布前冲突预检")
    @PreAuthorize("hasAuthority('schedule:query')")
    public Result<SchedulePlanPrecheckResultDTO> precheck(
            @Parameter(description = "方案ID") @PathVariable Long planId,
            @Parameter(description = "预检模式：STRICT/FORCE") @RequestParam(defaultValue = "STRICT") String mode) {
        return Result.ok(schedulePlanApplicationService.precheck(planId, mode));
    }

    @PostMapping("/{planId}/rollback")
    @Operation(summary = "回滚到指定排班方案版本")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<SchedulePlanPublishResultDTO> rollback(
            @Parameter(description = "方案ID") @PathVariable Long planId,
            @Parameter(description = "回滚模式：STRICT/FORCE") @RequestParam(defaultValue = "STRICT") String mode) {
        return Result.ok(schedulePlanApplicationService.rollback(planId, mode));
    }
}
