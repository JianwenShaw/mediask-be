package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.request.ai.SubmitAiReviewRequest;
import me.jianwen.mediask.api.security.CurrentUserProvider;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.schedule.domain.readmodel.AiDepartmentMetrics;
import me.jianwen.mediask.schedule.domain.readmodel.AiOverviewMetrics;
import me.jianwen.mediask.service.application.command.SubmitAiReviewCommand;
import me.jianwen.mediask.service.application.dto.ai.AiReviewResultDTO;
import me.jianwen.mediask.service.application.AiMetricsApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI指标", description = "AI问诊复核与指标统计")
@RequiredArgsConstructor
public class AiMetricsController {

    private final AiMetricsApplicationService aiMetricsApplicationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/reviews")
    @Operation(summary = "提交AI问诊复核", description = "医生对AI问诊结果提交复核评分")
    @PreAuthorize("hasAuthority('doctor')")
    public Result<AiReviewResultDTO> submitReview(@Valid @RequestBody SubmitAiReviewRequest request) {
        Long userId = currentUserProvider.currentUserId();
        SubmitAiReviewCommand command = new SubmitAiReviewCommand();
        command.setConversationId(request.getConversationId());
        command.setReviewScore(request.getReviewScore());
        command.setAdopted(request.getAdopted());
        command.setReviewComment(request.getReviewComment());
        return Result.ok(aiMetricsApplicationService.submitReview(userId, command));
    }

    @GetMapping("/metrics/overview")
    @Operation(summary = "AI指标总览", description = "管理员查看AI问诊全局指标")
    @PreAuthorize("hasAuthority('admin')")
    public Result<AiOverviewMetrics> getOverview(
            @Parameter(description = "统计日期，默认当天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(aiMetricsApplicationService.getOverviewMetrics(date));
    }

    @GetMapping("/metrics/departments")
    @Operation(summary = "AI指标分科室统计", description = "管理员查看AI问诊分科室统计")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<AiDepartmentMetrics>> getDepartmentMetrics(
            @Parameter(description = "统计日期，默认当天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(aiMetricsApplicationService.listDepartmentMetrics(date));
    }
}
