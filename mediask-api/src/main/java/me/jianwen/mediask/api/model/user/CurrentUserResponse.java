package me.jianwen.mediask.api.model.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 当前用户信息响应（API层）
 */
@Data
@Builder
public class CurrentUserResponse {
    private Long userId;
    private String username;
    private String phone;
    private Integer userType;
    private String realName;
    private Integer gender;
    private LocalDate birthDate;
    private String avatarUrl;
}
