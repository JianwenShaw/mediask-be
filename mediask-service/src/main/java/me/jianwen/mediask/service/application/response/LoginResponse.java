package me.jianwen.mediask.service.application.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 登录响应（应用层）
 */
@Value
@Builder
public class LoginResponse {
    Long userId;
    String username;
    Integer userType;
    List<String> authorities;
    String tokenType;
    Long expireAt;
    Long expiresIn;
    String token;
    String refreshToken;
    String refreshTokenId;
}
