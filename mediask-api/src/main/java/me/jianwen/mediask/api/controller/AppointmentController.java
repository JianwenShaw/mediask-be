package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.schedule.application.dto.AppointmentDTO;
import me.jianwen.mediask.schedule.application.dto.AppointmentResultDTO;
import me.jianwen.mediask.schedule.application.dto.AvailableSlotDTO;
import me.jianwen.mediask.schedule.application.request.CancelAppointmentRequest;
import me.jianwen.mediask.schedule.application.request.CreateAppointmentRequest;
import me.jianwen.mediask.schedule.application.service.AppointmentApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约挂号控制器
 *
 * @author jianwen
 */
@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "预约挂号", description = "预约挂号相关接口")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentApplicationService appointmentApplicationService;

    /**
     * 创建预约
     */
    @PostMapping
    @Operation(summary = "创建预约", description = "患者选择排班和时段进行挂号")
    @PreAuthorize("hasAuthority('patient')")
    public Result<AppointmentResultDTO> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        Long patientId = currentUserId();
        AppointmentResultDTO result = appointmentApplicationService.createAppointment(patientId, request);
        return Result.ok(result);
    }

    /**
     * 取消预约
     */
    @PostMapping("/cancel")
    @Operation(summary = "取消预约", description = "患者取消自己的预约")
    @PreAuthorize("hasAuthority('patient')")
    public Result<Void> cancelAppointment(
            @Valid @RequestBody CancelAppointmentRequest request) {
        Long patientId = currentUserId();
        appointmentApplicationService.cancelAppointment(patientId, request);
        return Result.ok();
    }

    /**
     * 支付预约
     */
    @PostMapping("/{appointmentId}/pay")
    @Operation(summary = "支付预约", description = "模拟支付预约（实际项目中对接支付网关）")
    @PreAuthorize("hasAuthority('patient')")
    public Result<Void> payAppointment(
            @Parameter(description = "预约ID") @PathVariable Long appointmentId) {
        appointmentApplicationService.payAppointment(appointmentId);
        return Result.ok();
    }

    /**
     * 标记已就诊
     */
    @PostMapping("/{appointmentId}/visited")
    @Operation(summary = "标记已就诊", description = "医生标记患者已就诊")
    @PreAuthorize("hasAuthority('doctor')")
    public Result<Void> markAsVisited(
            @Parameter(description = "预约ID") @PathVariable Long appointmentId) {
        appointmentApplicationService.markAsVisited(appointmentId);
        return Result.ok();
    }

    /**
     * 查询我的预约列表
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的预约", description = "查询当前登录患者的预约列表")
    @PreAuthorize("hasAuthority('patient')")
    public Result<List<AppointmentDTO>> listMyAppointments(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long patientId = currentUserId();
        List<AppointmentDTO> appointments = appointmentApplicationService.listPatientAppointments(patientId, startDate, endDate);
        return Result.ok(appointments);
    }

    /**
     * 查询待支付预约
     */
    @GetMapping("/my/unpaid")
    @Operation(summary = "查询待支付预约", description = "查询当前登录患者的待支付预约")
    @PreAuthorize("hasAuthority('patient')")
    public Result<List<AppointmentDTO>> listUnpaidAppointments() {
        Long patientId = currentUserId();
        List<AppointmentDTO> appointments = appointmentApplicationService.listUnpaidAppointments(patientId);
        return Result.ok(appointments);
    }

    /**
     * 查询预约详情
     */
    @GetMapping("/{appointmentId}")
    @Operation(summary = "查询预约详情")
    public Result<AppointmentDTO> getAppointment(
            @Parameter(description = "预约ID") @PathVariable Long appointmentId) {
        AppointmentDTO appointment = appointmentApplicationService.getAppointment(appointmentId);
        return Result.ok(appointment);
    }

    /**
     * 查询可预约时段
     */
    @GetMapping("/slots/available")
    @Operation(summary = "查询可预约时段", description = "查询指定排班的可预约时段列表")
    public Result<List<AvailableSlotDTO>> listAvailableSlots(
            @Parameter(description = "排班ID") @RequestParam Long scheduleId) {
        List<AvailableSlotDTO> slots = appointmentApplicationService.listAvailableSlots(scheduleId);
        return Result.ok(slots);
    }

    /**
     * 获取当前登录用户ID
     */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) return id;
        if (principal instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
