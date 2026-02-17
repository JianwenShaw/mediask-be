package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.model.department.DepartmentResponse;
import me.jianwen.mediask.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/department")
@Tag(name = "科室数据", description = "科室数据 CRUD")
@RequiredArgsConstructor
public class DepartmentController {
    @GetMapping("/departments")
    @Operation(summary = "查询科室列表", description = "返回科室列表信息")
    @PreAuthorize("hasRole('admin')")
    public Result<List<DepartmentResponse>> listDepartments() {
        return Result.ok();
    }
}
