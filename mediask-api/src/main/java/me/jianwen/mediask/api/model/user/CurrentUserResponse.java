package me.jianwen.mediask.api.model.user;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * 当前登录用户信息（脱敏）
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


