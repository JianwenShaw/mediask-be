package me.jianwen.mediask.service.application.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * 当前用户信息响应（应用层）
 */
@Value
@Builder
public class CurrentUserResponse {
    Long userId;
    String username;
    String phone;
    Integer userType;
    String realName;
    Integer gender;
    LocalDate birthDate;
    String avatarUrl;
}
