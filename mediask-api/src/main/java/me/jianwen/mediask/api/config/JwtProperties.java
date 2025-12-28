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

    public void validate() {
        Assert.hasText(secret, "security.jwt.secret 不能为空");
        Assert.isTrue(secret.length() >= 32, "security.jwt.secret 长度需至少 32 字符");
        Assert.isTrue(expireSeconds > 0, "security.jwt.expire-seconds 必须大于 0");
    }
}

