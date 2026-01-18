package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.model.appointment.CancelAppointmentRequest;
import me.jianwen.mediask.api.model.appointment.CreateAppointmentRequest;
import me.jianwen.mediask.api.model.appointment.AppointmentResponse;
import me.jianwen.mediask.api.model.appointment.AppointmentResultResponse;
import me.jianwen.mediask.api.model.appointment.AvailableSlotResponse;
import me.jianwen.mediask.api.mapper.AppointmentApiMapper;
import me.jianwen.mediask.api.security.CurrentUserProvider;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.service.AppointmentApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final CurrentUserProvider currentUserProvider;
    private final AppointmentApiMapper appointmentApiMapper;

    /**
     * 创建预约
     */
    @PostMapping
    @Operation(summary = "创建预约", description = "患者选择排班和时段进行挂号")
    @PreAuthorize("hasAuthority('patient')")
    public Result<AppointmentResultResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request) {
        Long patientId = currentUserId();
        var serviceRequest = appointmentApiMapper.toService(request);
        me.jianwen.mediask.service.application.response.AppointmentResultResponse result =
            appointmentApplicationService.createAppointment(patientId, serviceRequest);
        return Result.ok(appointmentApiMapper.toResponse(result));
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
        var serviceRequest = appointmentApiMapper.toService(request);
        appointmentApplicationService.cancelAppointment(patientId, serviceRequest);
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
    public Result<List<AppointmentResponse>> listMyAppointments(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long patientId = currentUserId();
        List<me.jianwen.mediask.service.application.response.AppointmentResponse> appointments =
            appointmentApplicationService.listPatientAppointments(patientId, startDate, endDate);
        return Result.ok(appointments.stream().map(appointmentApiMapper::toResponse).toList());
    }

    /**
     * 查询待支付预约
     */
    @GetMapping("/my/unpaid")
    @Operation(summary = "查询待支付预约", description = "查询当前登录患者的待支付预约")
    @PreAuthorize("hasAuthority('patient')")
    public Result<List<AppointmentResponse>> listUnpaidAppointments() {
        Long patientId = currentUserId();
        List<me.jianwen.mediask.service.application.response.AppointmentResponse> appointments =
            appointmentApplicationService.listUnpaidAppointments(patientId);
        return Result.ok(appointments.stream().map(appointmentApiMapper::toResponse).toList());
    }

    /**
     * 查询预约详情
     */
    @GetMapping("/{appointmentId}")
    @Operation(summary = "查询预约详情")
    public Result<AppointmentResponse> getAppointment(
            @Parameter(description = "预约ID") @PathVariable Long appointmentId) {
        me.jianwen.mediask.service.application.response.AppointmentResponse appointment =
            appointmentApplicationService.getAppointment(appointmentId);
        return Result.ok(appointmentApiMapper.toResponse(appointment));
    }

    /**
     * 查询可预约时段
     */
    @GetMapping("/slots/available")
    @Operation(summary = "查询可预约时段", description = "查询指定排班的可预约时段列表")
    public Result<List<AvailableSlotResponse>> listAvailableSlots(
            @Parameter(description = "排班ID") @RequestParam Long scheduleId) {
        List<me.jianwen.mediask.service.application.response.AvailableSlotResponse> slots =
            appointmentApplicationService.listAvailableSlots(scheduleId);
        return Result.ok(slots.stream().map(appointmentApiMapper::toResponse).toList());
    }

    /**
     * 获取当前登录用户ID
     */
    private Long currentUserId() {
        return currentUserProvider.currentUserId();
    }

}
