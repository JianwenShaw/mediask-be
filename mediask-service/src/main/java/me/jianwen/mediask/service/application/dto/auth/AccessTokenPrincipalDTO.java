package me.jianwen.mediask.service.application.dto.auth;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Access Token 解析后的认证主体
 */
@Value
@Builder
public class AccessTokenPrincipalDTO {
    Long userId;
    List<String> authorities;
}
