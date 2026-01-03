package me.jianwen.mediask.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * JWT 基础配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * 签名密钥（HS256），长度建议 >= 32 字节
     */
    private String secret = "mediask-dev-secret-please-change-32chars-min";

    /**
     * 签发方标识
     */
    private String issuer = "mediask";

    /**
     * 过期时间（秒）
     */
    private long expireSeconds = 604800L;

    /**
     * Refresh Token 过期时间（秒）
     * <p>
     * 默认 30 天。refresh token 仅用于换取新的 access token，本身不应作为 API 访问凭证。
     * </p>
     */
    private long refreshExpireSeconds = 2592000L;

    public void validate() {
        Assert.hasText(secret, "security.jwt.secret 不能为空");
        Assert.isTrue(secret.length() >= 32, "security.jwt.secret 长度需至少 32 字符");
        Assert.isTrue(expireSeconds > 0, "security.jwt.expire-seconds 必须大于 0");
        Assert.isTrue(refreshExpireSeconds > 0, "security.jwt.refresh-expire-seconds 必须大于 0");
    }
}

