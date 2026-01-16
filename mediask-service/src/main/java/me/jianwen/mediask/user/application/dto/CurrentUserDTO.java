package me.jianwen.mediask.user.application.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * 当前用户信息 DTO
 */
@Value
@Builder
public class CurrentUserDTO {
    Long userId;
    String username;
    String phone;
    Integer userType;
    String realName;
    Integer gender;
    LocalDate birthDate;
    String avatarUrl;
}

