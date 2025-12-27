package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.response.R;
import me.jianwen.mediask.schedule.application.command.AutoScheduleCommand;
import me.jianwen.mediask.schedule.application.command.CreateScheduleCommand;
import me.jianwen.mediask.schedule.application.service.ScheduleApplicationService;
import me.jianwen.mediask.schedule.domain.entity.DoctorSchedule;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 创建排班
     */
    @PostMapping
    @Operation(summary = "创建排班", description = "手动创建单个排班")
    @PreAuthorize("hasAuthority('schedule:create')")
    public R<Long> createSchedule(@Validated @RequestBody CreateScheduleCommand command) {
        Long scheduleId = scheduleApplicationService.createSchedule(command);
        return R.ok(scheduleId);
    }

    /**
     * 自动排班
     */
    @PostMapping("/auto")
    @Operation(summary = "自动排班", description = "根据规则自动批量生成排班")
    @PreAuthorize("hasAuthority('schedule:auto')")
    public R<List<Long>> autoSchedule(@Validated @RequestBody AutoScheduleCommand command) {
        List<Long> scheduleIds = scheduleApplicationService.autoSchedule(command);
        return R.ok(scheduleIds);
    }

    /**
     * 停诊
     */
    @PostMapping("/{scheduleId}/close")
    @Operation(summary = "停诊", description = "医生请假、调休等导致停诊")
    @PreAuthorize("hasAuthority('schedule:update')")
    public R<Void> closeSchedule(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId,
            @Parameter(description = "停诊原因") @RequestParam(required = false) String reason) {
        scheduleApplicationService.closeSchedule(scheduleId, reason);
        return R.ok();
    }

    /**
     * 开诊
     */
    @PostMapping("/{scheduleId}/open")
    @Operation(summary = "开诊", description = "取消停诊，恢复排班")
    @PreAuthorize("hasAuthority('schedule:update')")
    public R<Void> openSchedule(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId) {
        scheduleApplicationService.openSchedule(scheduleId);
        return R.ok();
    }

    /**
     * 调整号源数量
     */
    @PutMapping("/{scheduleId}/slots")
    @Operation(summary = "调整号源数量", description = "管理员手动调整总号源数")
    @PreAuthorize("hasAuthority('schedule:update')")
    public R<Void> adjustTotalSlots(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId,
            @Parameter(description = "新的总号源数") @RequestParam int totalSlots) {
        scheduleApplicationService.adjustTotalSlots(scheduleId, totalSlots);
        return R.ok();
    }

    /**
     * 查询排班详情
     */
    @GetMapping("/{scheduleId}")
    @Operation(summary = "查询排班详情")
    public R<DoctorSchedule> getSchedule(
            @Parameter(description = "排班ID") @PathVariable Long scheduleId) {
        DoctorSchedule schedule = scheduleApplicationService.getScheduleById(scheduleId);
        return R.ok(schedule);
    }

    /**
     * 查询医生排班列表
     */
    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "查询医生排班列表", description = "查询医生在日期范围内的所有排班")
    public R<List<DoctorSchedule>> listDoctorSchedules(
            @Parameter(description = "医生ID") @PathVariable Long doctorId,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<DoctorSchedule> schedules = scheduleApplicationService
                .listSchedulesByDoctorAndDateRange(doctorId, startDate, endDate);
        return R.ok(schedules);
    }

    /**
     * 查询可预约的排班
     */
    @GetMapping("/available")
    @Operation(summary = "查询可预约排班", description = "查询指定日期和时段的可预约排班列表")
    public R<List<DoctorSchedule>> listAvailableSchedules(
            @Parameter(description = "日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "时段代码：1上午 2下午 3晚上") @RequestParam Integer periodCode) {

        TimePeriod period = TimePeriod.fromCode(periodCode);
        List<DoctorSchedule> schedules = scheduleApplicationService.listOpenSchedules(date, period);
        return R.ok(schedules);
    }
}
