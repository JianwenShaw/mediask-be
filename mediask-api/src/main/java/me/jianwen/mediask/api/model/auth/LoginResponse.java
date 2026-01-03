package me.jianwen.mediask.api.model.auth;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 登录响应
 */
@Value
@Builder
public class LoginResponse {
    Long userId;
    String username;
    Integer userType;
    List<String> authorities;

    /**
     * token 类型（默认 Bearer）
     */
    @Builder.Default
    String tokenType = "Bearer";

    String token;

    /**
     * access token 过期时间（秒级时间戳，兼容字段）
     */
    Long expireAt;

    /**
     * access token 剩余有效期（秒）
     * 前端可用该字段计算 expiresAt（ms）以避免单位歧义。
     */
    Long expiresIn;

    /**
     * refresh token（用于换取新的 access token）
     */
    String refreshToken;
}

