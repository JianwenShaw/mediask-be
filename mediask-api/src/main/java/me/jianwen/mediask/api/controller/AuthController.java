package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.model.auth.LoginRequest;
import me.jianwen.mediask.api.model.auth.LoginResponse;
import me.jianwen.mediask.api.model.auth.RegisterRequest;
import me.jianwen.mediask.api.service.AuthService;
import me.jianwen.mediask.common.result.Result;
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

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return Result.ok(userId);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名或手机号登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.ok(response);
    }
}

