package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.model.user.CurrentUserResponse;
import me.jianwen.mediask.api.service.UserService;
import me.jianwen.mediask.common.result.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户", description = "用户信息相关接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public Result<CurrentUserResponse> me() {
        Long userId = currentUserId();
        return Result.ok(userService.getCurrentUser(userId));
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


