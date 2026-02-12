package me.jianwen.mediask.common.dto.user;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * 用户信息DTO（Service层与API层共享）
 */
@Value
@Builder
public class UserDTO {
    Long userId;
    String username;
    String phone;
    Integer userType;
    String realName;
    Integer gender;
    LocalDate birthDate;
    String avatarUrl;
}
