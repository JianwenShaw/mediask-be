package me.jianwen.mediask.user.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 登录响应 DTO
 */
@Value
@Builder
public class LoginResponseDTO {
    Long userId;
    String username;
    Integer userType;
    List<String> authorities;
    String tokenType;
    String token;
    Long expireAt;
    Long expiresIn;
    String refreshToken;
}

