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
    String token;
    Long expireAt;
}

