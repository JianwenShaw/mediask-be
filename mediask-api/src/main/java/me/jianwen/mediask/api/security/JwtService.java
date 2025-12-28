package me.jianwen.mediask.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.config.JwtProperties;
import me.jianwen.mediask.dal.enums.UserTypeEnum;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JWT 生成与解析服务
 */
@Component
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    /**
     * 生成用户访问令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param userType 用户类型
     * @return token 及过期时间
     */
    public JwtToken generateToken(Long userId, String username, UserTypeEnum userType, List<String> authorities) {
        properties.validate();

        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(properties.getExpireSeconds());

        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);

        String token = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuer(properties.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expireAt))
                .claim("username", username)
                .claim("userType", userType != null ? userType.getCode() : null)
                .claim("perms", authorities)
                .signWith(Keys.hmacShaKeyFor(keyBytes), SignatureAlgorithm.HS256)
                .compact();

        return new JwtToken(token, expireAt.getEpochSecond());
    }

    /**
     * 解析并验证 token
     */
    public JwtPayload parseToken(String token) {
        properties.validate();
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        Jws<Claims> jws = Jwts.parser()
                .setSigningKey(Keys.hmacShaKeyFor(keyBytes))
                .build()
                .parseClaimsJws(token);

        Claims claims = jws.getBody();
        Long userId = Optional.ofNullable(claims.getSubject()).map(Long::valueOf).orElse(null);
        String username = claims.get("username", String.class);
        Integer userTypeCode = claims.get("userType", Integer.class);
        List<String> perms = claims.get("perms", List.class);

        return new JwtPayload(userId, username, userTypeCode, perms);
    }

    /**
     * 简单 token 返回体
     *
     * @param token    JWT 字符串
     * @param expireAt 过期时间（秒级时间戳）
     */
    public record JwtToken(String token, long expireAt) {
    }

    /**
     * token 解析后的载荷
     */
    public record JwtPayload(Long userId, String username, Integer userType, List<String> authorities) {
    }
}
