package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.mapper.AuthzApiMapper;
import me.jianwen.mediask.api.request.authz.UpdateUserRolesRequest;
import me.jianwen.mediask.api.response.authz.PermissionResponse;
import me.jianwen.mediask.api.response.authz.RoleResponse;
import me.jianwen.mediask.api.response.authz.UserRolesResponse;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.AuthorizationQueryService;
import me.jianwen.mediask.service.application.UserRoleApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/authz")
@Tag(name = "权限管理", description = "管理员管理角色与用户权限接口")
@RequiredArgsConstructor
public class AuthzController {

    private final AuthorizationQueryService authorizationQueryService;
    private final UserRoleApplicationService userRoleApplicationService;
    private final AuthzApiMapper authzApiMapper;

    @GetMapping("/roles")
    @Operation(summary = "查询角色列表", description = "返回角色基础信息及绑定的权限编码")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<RoleResponse>> listRoles() {
        List<RoleResponse> response = authorizationQueryService.listRoles().stream()
                .map(authzApiMapper::toResponse)
                .toList();
        return Result.ok(response);
    }

    @GetMapping("/permissions")
    @Operation(summary = "查询权限列表", description = "返回系统内全部权限编码定义")
    @PreAuthorize("hasAuthority('admin')")
    public Result<List<PermissionResponse>> listPermissions() {
        List<PermissionResponse> response = authorizationQueryService.listPermissions().stream()
                .map(authzApiMapper::toResponse)
                .toList();
        return Result.ok(response);
    }

    @GetMapping("/users/{userId}/roles")
    @Operation(summary = "查询用户角色", description = "返回指定用户当前角色编码列表")
    @PreAuthorize("hasAuthority('admin')")
    public Result<UserRolesResponse> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        return Result.ok(authzApiMapper.toResponse(authorizationQueryService.getUserRoles(userId)));
    }

    @PutMapping("/users/{userId}/roles")
    @Operation(summary = "更新用户角色", description = "覆盖式更新用户角色编码列表")
    @PreAuthorize("hasAuthority('admin')")
    public Result<Void> updateUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        userRoleApplicationService.updateUserRoles(userId, authzApiMapper.toService(request));
        return Result.ok();
    }
}
