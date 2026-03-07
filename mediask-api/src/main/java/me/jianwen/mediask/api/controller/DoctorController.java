package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.mapper.DoctorApiMapper;
import me.jianwen.mediask.api.request.doctor.CreateDoctorRequest;
import me.jianwen.mediask.api.response.doctor.DoctorResponse;
import me.jianwen.mediask.api.request.doctor.UpdateDoctorRequest;
import me.jianwen.mediask.common.model.PageResult;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.command.DoctorPageQueryCommand;
import me.jianwen.mediask.service.application.dto.doctor.DoctorDTO;
import me.jianwen.mediask.service.application.DoctorApplicationService;
import me.jianwen.mediask.service.application.DoctorQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@Tag(name = "医生管理", description = "医生档案管理接口")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorApplicationService doctorApplicationService;
    private final DoctorQueryService doctorQueryService;
    private final DoctorApiMapper doctorApiMapper;

    @PostMapping
    @Operation(summary = "创建医生档案")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Long> createDoctor(@Validated @RequestBody CreateDoctorRequest request) {
        return Result.ok(doctorApplicationService.createDoctor(doctorApiMapper.toService(request)));
    }

    @PutMapping("/{doctorId}")
    @Operation(summary = "更新医生档案")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> updateDoctor(
            @Parameter(description = "医生ID") @PathVariable Long doctorId,
            @Validated @RequestBody UpdateDoctorRequest request) {
        doctorApplicationService.updateDoctor(doctorId, doctorApiMapper.toService(request));
        return Result.ok();
    }

    @PutMapping("/{doctorId}/status")
    @Operation(summary = "启停医生档案")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> updateDoctorStatus(
            @Parameter(description = "医生ID") @PathVariable Long doctorId,
            @Parameter(description = "状态 0-停用 1-启用") @RequestParam Integer status) {
        doctorApplicationService.updateDoctorStatus(doctorId, status);
        return Result.ok();
    }

    @GetMapping("/{doctorId}")
    @Operation(summary = "医生详情")
    @PreAuthorize("hasAuthority('admin')")
    public Result<DoctorResponse> getDoctor(
            @Parameter(description = "医生ID") @PathVariable Long doctorId) {
        DoctorDTO doctorDTO = doctorQueryService.getDoctor(doctorId);
        return Result.ok(doctorApiMapper.toResponse(doctorDTO));
    }

    @GetMapping
    @Operation(summary = "分页查询医生档案")
    @PreAuthorize("hasAuthority('admin')")
    public Result<PageResult<DoctorResponse>> pageDoctors(
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "医生编码") @RequestParam(required = false) String doctorCode,
            @Parameter(description = "医院ID") @RequestParam(required = false) Long hospitalId,
            @Parameter(description = "科室ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "状态 0-停用 1-启用") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键字（医生编码/职称/执业证号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {
        DoctorPageQueryCommand command = new DoctorPageQueryCommand();
        command.setUserId(userId);
        command.setDoctorCode(doctorCode);
        command.setHospitalId(hospitalId);
        command.setDepartmentId(departmentId);
        command.setStatus(status);
        command.setKeyword(keyword);
        command.setPageNum(pageNum);
        command.setPageSize(pageSize);

        PageResult<DoctorDTO> page = doctorQueryService.pageDoctors(command);
        List<DoctorResponse> list = page.getList().stream().map(doctorApiMapper::toResponse).toList();
        return Result.ok(PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), list));
    }
}
