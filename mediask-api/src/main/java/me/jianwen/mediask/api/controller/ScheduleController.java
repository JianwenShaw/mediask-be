package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.mapper.ScheduleApiMapper;
import me.jianwen.mediask.api.mapper.ScheduleTemplateApiMapper;
import me.jianwen.mediask.api.model.schedule.AutoScheduleRequest;
import me.jianwen.mediask.api.model.schedule.CreateScheduleRequest;
import me.jianwen.mediask.api.model.schedule.GenerateScheduleFromTemplateRequest;
import me.jianwen.mediask.api.model.schedule.ScheduleResponse;
import me.jianwen.mediask.common.dto.schedule.AutoSchedulePlanDTO;
import me.jianwen.mediask.common.dto.schedule.ScheduleDTO;
import me.jianwen.mediask.common.model.PageResult;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.command.AutoScheduleCommand;
import me.jianwen.mediask.service.application.service.ScheduleApplicationService;
import me.jianwen.mediask.service.application.service.ScheduleTemplateApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班管理控制器
 *
 * @author jianwen
 */
@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "排班管理", description = "医生排班管理相关接口")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleApplicationService scheduleApplicationService;
    private final ScheduleTemplateApplicationService scheduleTemplateApplicationService;
    private final ScheduleApiMapper scheduleApiMapper;
    private final ScheduleTemplateApiMapper scheduleTemplateApiMapper;

    /**
     * 创建排班
     */
    @PostMapping
    @Operation(summary = "创建排班", description = "手动创建单个排班")
    @PreAuthorize("hasAuthority('schedule:create')")
    public Result<Long> createSchedule(@Validated @RequestBody CreateScheduleRequest request) {
        var serviceRequest = scheduleApiMapper.toService(request);
        Long scheduleId = scheduleApplicationService.createSchedule(serviceRequest);
        return Result.ok(scheduleId);
    }

    /**
     * 自动排班
     */
    @PostMapping("/auto")
    @Operation(summary = "自动排班", description = "根据规则自动批量生成排班")
    @PreAuthorize("hasAuthority('schedule:auto')")
    public Result<AutoSchedulePlanDTO> autoSchedule(@Validated @RequestBody AutoScheduleRequest request) {
        AutoScheduleCommand serviceRequest = toAutoScheduleCommand(request);
        AutoSchedulePlanDTO result = scheduleApplicationService.autoSchedule(serviceRequest);
        return Result.ok(result);
    }

    /**
     * 根据模板生成排班实例
     */
    @PostMapping("/generate")
    @Operation(summary = "根据模板生成排班", description = "按模板和日期范围生成排班实例")
    @PreAuthorize("hasAuthority('schedule:create')")
    public Result<List<Long>> generateSchedules(@Validated @RequestBody GenerateScheduleFromTemplateRequest request) {
        var serviceRequest = scheduleTemplateApiMapper.toService(request);
        return Result.ok(scheduleTemplateApplicationService.generateSchedules(serviceRequest));
    }

    /**
     * 停诊
     */
    @PostMapping("/{scheduleId}/close")
    @Operation(summary = "停诊", description = "医生请假、调休等导致停诊")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<Void> closeSchedule(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId,
            @Parameter(description = "停诊原因") @RequestParam(required = false) String reason) {
        scheduleApplicationService.closeSchedule(scheduleId, reason);
        return Result.ok();
    }

    /**
     * 开诊
     */
    @PostMapping("/{scheduleId}/open")
    @Operation(summary = "开诊", description = "取消停诊，恢复排班")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<Void> openSchedule(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId) {
        scheduleApplicationService.openSchedule(scheduleId);
        return Result.ok();
    }

    /**
     * 调整号源数量
     */
    @PutMapping("/{scheduleId}/slots")
    @Operation(summary = "调整号源数量", description = "管理员手动调整总号源数")
    @PreAuthorize("hasAuthority('schedule:update')")
    public Result<Void> adjustTotalSlots(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId,
            @Parameter(description = "新的总号源数") @RequestParam int totalSlots) {
        scheduleApplicationService.adjustTotalSlots(scheduleId, totalSlots);
        return Result.ok();
    }

    /**
     * 查询排班详情
     */
    @GetMapping("/{scheduleId}")
    @Operation(summary = "查询排班详情")
    public Result<ScheduleResponse> getSchedule(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId) {
        ScheduleDTO schedule = scheduleApplicationService.getScheduleById(scheduleId);
        return Result.ok(scheduleApiMapper.toResponse(schedule));
    }

    /**
     * 查询医生排班列表
     */
    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "查询医生排班列表", description = "查询医生在日期范围内的所有排班")
    public Result<List<ScheduleResponse>> listDoctorSchedules(
            @Parameter(description = "医生ID") @PathVariable Long doctorId,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<ScheduleDTO> schedules = scheduleApplicationService
                .listSchedulesByDoctorAndDateRange(doctorId, startDate, endDate);
        return Result.ok(schedules.stream().map(scheduleApiMapper::toResponse).toList());
    }

    /**
     * 查询可预约的排班
     */
    @GetMapping("/available")
    @Operation(summary = "查询可预约排班", description = "查询指定日期和时段的可预约排班列表")
    public Result<List<ScheduleResponse>> listAvailableSchedules(
            @Parameter(description = "日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "时段代码：1上午 2下午 3晚上") @RequestParam Integer periodCode) {

        List<ScheduleDTO> schedules = scheduleApplicationService.listOpenSchedules(date, periodCode);
        return Result.ok(schedules.stream().map(scheduleApiMapper::toResponse).toList());
    }

    /**
     * 分页查询排班列表
     */
    @GetMapping
    @Operation(summary = "分页查询排班列表", description = "支持多条件筛选和分页")
    @PreAuthorize("hasAuthority('schedule:query')")
    public Result<PageResult<ScheduleResponse>> listSchedulesPaged(
            @Parameter(description = "医生ID") @RequestParam(required = false) Long doctorId,
            @Parameter(description = "科室ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "状态：0停诊 1开放 2约满 3过期") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {

        PageResult<ScheduleDTO> result = scheduleApplicationService.listSchedulesPaged(
                doctorId, departmentId, startDate, endDate, status, pageNum, pageSize);

        List<ScheduleResponse> responses = result.getList().stream()
                .map(scheduleApiMapper::toResponse)
                .toList();

        return Result.ok(new PageResult<>(result.getTotal(), result.getPageNum(), result.getPageSize(), responses));
    }

    /**
     * 删除排班
     */
    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "删除排班", description = "逻辑删除排班，会检查关联预约")
    @PreAuthorize("hasAuthority('schedule:delete')")
    public Result<Void> deleteSchedule(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId,
            @Parameter(description = "是否强制删除（取消关联预约）") @RequestParam(defaultValue = "false") Boolean force) {
        scheduleApplicationService.deleteSchedule(scheduleId, force);
        return Result.ok();
    }

    /**
     * 批量删除排班
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除排班", description = "按日期范围批量删除排班")
    @PreAuthorize("hasAuthority('schedule:delete')")
    public Result<Integer> batchDeleteSchedules(
            @Parameter(description = "医生ID") @RequestParam Long doctorId,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "是否强制删除") @RequestParam(defaultValue = "false") Boolean force) {
        int count = scheduleApplicationService.batchDeleteSchedules(doctorId, startDate, endDate, force);
        return Result.ok(count);
    }

    private AutoScheduleCommand toAutoScheduleCommand(AutoScheduleRequest request) {
        AutoScheduleCommand command = new AutoScheduleCommand();
        command.setDepartmentId(request.getDepartmentId());
        if (request.getDateRange() != null) {
            command.setStartDate(request.getDateRange().getStartDate());
            command.setEndDate(request.getDateRange().getEndDate());
        }
        command.setDoctorIds(request.getDoctorIds());
        command.setPeriods(request.getPeriods());
        if (request.getDemand() != null && request.getDemand().getByDatePeriod() != null) {
            command.setDemands(request.getDemand().getByDatePeriod().stream()
                    .map(d -> new AutoScheduleCommand.DemandItemCommand(
                            d.getDate(),
                            d.getPeriodCode(),
                            d.getRequiredDoctors(),
                            d.getMinSeniorDoctors()))
                    .toList());
        }
        if (request.getHardConstraints() != null) {
            command.setHardConstraints(new AutoScheduleCommand.HardConstraints(
                    request.getHardConstraints().getMaxConsecutiveDays(),
                    request.getHardConstraints().getMaxShiftsPerWeek(),
                    request.getHardConstraints().getMinRestHoursBetweenShifts(),
                    request.getHardConstraints().getExcludeHolidays(),
                    request.getHardConstraints().getHolidayPolicy(),
                    request.getHardConstraints().getHolidayReductionFactor()
            ));
        }
        if (request.getSoftGoals() != null) {
            command.setSoftGoals(new AutoScheduleCommand.SoftGoals(
                    request.getSoftGoals().getFairnessWeight(),
                    request.getSoftGoals().getPreferenceWeight(),
                    request.getSoftGoals().getContinuityWeight(),
                    request.getSoftGoals().getSeniorCoverageWeight(),
                    request.getSoftGoals().getWeekendBalanceWeight()
            ));
        }
        if (request.getSolverConfig() != null) {
            command.setSolverConfig(new AutoScheduleCommand.SolverConfig(
                    request.getSolverConfig().getStrategy(),
                    request.getSolverConfig().getMaxIterations(),
                    request.getSolverConfig().getTimeLimitMs(),
                    request.getSolverConfig().getSeed()
            ));
        }
        return command;
    }
}
