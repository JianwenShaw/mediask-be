package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.mapper.AuthApiMapper;
import me.jianwen.mediask.api.model.auth.LoginRequest;
import me.jianwen.mediask.api.model.auth.RefreshTokenRequest;
import me.jianwen.mediask.api.model.auth.RegisterRequest;
import me.jianwen.mediask.api.security.CurrentUserProvider;
import me.jianwen.mediask.api.security.SecurityAuditUtil;
import me.jianwen.mediask.common.dto.auth.LoginDTO;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.service.AuthApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与用户注册接口
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证与注册", description = "登录、注册基础接口")
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthApiMapper authApiMapper;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册成功后自动登录，返回完整的认证信息")
    public Result<LoginDTO> register(@Valid @RequestBody RegisterRequest apiRequest) {
        var serviceRequest = authApiMapper.toService(apiRequest);
        LoginDTO dto = authApplicationService.register(serviceRequest);
        return Result.ok(dto);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名或手机号登录")
    public Result<LoginDTO> login(@Valid @RequestBody LoginRequest apiRequest, HttpServletRequest request) {
        var serviceRequest = authApiMapper.toService(apiRequest);
        serviceRequest.setClientIp(SecurityAuditUtil.clientIp(request));
        LoginDTO dto = authApplicationService.login(serviceRequest);
        return Result.ok(dto);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用 refreshToken 换取新的 access token（并轮换 refresh token）")
    public Result<LoginDTO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginDTO dto = authApplicationService.refresh(request.getRefreshToken());
        return Result.ok(dto);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "撤销当前用户的 Refresh Token，使会话失效")
    @SecurityRequirement(name = "bearerAuth")
    public Result<Void> logout() {
        Long userId = currentUserId();
        if (userId == null) {
            return Result.ok();
        }
        authApplicationService.logout(userId);
        return Result.ok();
    }

    private Long currentUserId() {
        return currentUserProvider.currentUserId();
    }

}
