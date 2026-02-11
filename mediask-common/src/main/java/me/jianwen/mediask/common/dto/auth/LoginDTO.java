package me.jianwen.mediask.common.dto.auth;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 登录响应DTO（Service层与API层共享）
 */
@Value
@Builder
public class LoginDTO {
    Long userId;
    String username;
    Integer userType;
    List<String> authorities;
    String tokenType;
    String token;
    Long expireAt;
    Long expiresIn;
    String refreshToken;
    String refreshTokenId;
}
