package me.jianwen.mediask.api.model.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 登录响应（API层）
 */
@Data
@Builder
public class LoginResponse {
    private Long userId;
    private String username;
    private Integer userType;
    private List<String> authorities;
    private String tokenType;
    private String token;
    private Long expireAt;
    private Long expiresIn;
    private String refreshToken;
    private String refreshTokenId;
}
