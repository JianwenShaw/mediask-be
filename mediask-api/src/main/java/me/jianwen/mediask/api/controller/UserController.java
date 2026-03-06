package me.jianwen.mediask.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.security.CurrentUserProvider;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.service.application.dto.user.UserDTO;
import me.jianwen.mediask.service.application.UserApplicationService;
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

    private final UserApplicationService userApplicationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me")
    @Operation(summary = "获取当前登录用户信息")
    public Result<UserDTO> me() {
        Long userId = currentUserId();
        UserDTO dto = userApplicationService.getCurrentUser(userId);
        return Result.ok(dto);
    }

    private Long currentUserId() {
        return currentUserProvider.currentUserId();
    }

}
