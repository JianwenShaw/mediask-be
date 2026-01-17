package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.model.auth.LoginRequest;
import me.jianwen.mediask.api.model.auth.LoginResponse;
import me.jianwen.mediask.api.model.auth.LogoutRequest;
import me.jianwen.mediask.api.model.auth.RefreshTokenRequest;
import me.jianwen.mediask.api.model.auth.RegisterRequest;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.user.application.dto.LoginResponseDTO;
import me.jianwen.mediask.user.application.service.AuthApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Long> register(@Valid @RequestBody RegisterRequest apiRequest) {
        me.jianwen.mediask.user.application.request.RegisterRequest serviceRequest = new me.jianwen.mediask.user.application.request.RegisterRequest();
        serviceRequest.setUsername(apiRequest.getUsername());
        serviceRequest.setPassword(apiRequest.getPassword());
        serviceRequest.setPhone(apiRequest.getPhone());
        serviceRequest.setUserType(apiRequest.getUserType());
        serviceRequest.setRealName(apiRequest.getRealName());
        serviceRequest.setGender(apiRequest.getGender());
        serviceRequest.setBirthDate(apiRequest.getBirthDate());
        serviceRequest.setAvatarUrl(apiRequest.getAvatarUrl());
        
        Long userId = authApplicationService.register(serviceRequest);
        return Result.ok(userId);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名或手机号登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest apiRequest) {
        me.jianwen.mediask.user.application.request.LoginRequest serviceRequest = new me.jianwen.mediask.user.application.request.LoginRequest();
        serviceRequest.setAccount(apiRequest.getAccount());
        serviceRequest.setPassword(apiRequest.getPassword());
        
        LoginResponseDTO dto = authApplicationService.login(serviceRequest);
        LoginResponse response = LoginResponse.builder()
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .userType(dto.getUserType())
                .authorities(dto.getAuthorities())
                .tokenType(dto.getTokenType())
                .token(dto.getToken())
                .expireAt(dto.getExpireAt())
                .expiresIn(dto.getExpiresIn())
                .refreshToken(dto.getRefreshToken())
                .refreshTokenId(dto.getRefreshTokenId())
                .build();
        return Result.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用 refreshToken 换取新的 access token（并轮换 refresh token）")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponseDTO dto = authApplicationService.refresh(request.getRefreshToken());
        LoginResponse response = LoginResponse.builder()
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .userType(dto.getUserType())
                .authorities(dto.getAuthorities())
                .tokenType(dto.getTokenType())
                .token(dto.getToken())
                .expireAt(dto.getExpireAt())
                .expiresIn(dto.getExpiresIn())
                .refreshToken(dto.getRefreshToken())
                .refreshTokenId(dto.getRefreshTokenId())
                .build();
        return Result.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "撤销 Refresh Token，使 Token 失效")
    @SecurityRequirement(name = "bearerAuth")
    public Result<Void> logout(@RequestBody LogoutRequest request) {
        Long userId = currentUserId();
        if (userId == null) {
            return Result.ok();
        }

        if (request.getRefreshTokenId() == null || request.getRefreshTokenId().isBlank()) {
            // 登出所有设备
            authApplicationService.logoutAll(userId);
        } else {
            // 仅登出当前设备
            authApplicationService.logout(userId, request.getRefreshTokenId());
        }
        return Result.ok();
    }

    private static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) return id;
        if (principal instanceof String str) {
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }
}

