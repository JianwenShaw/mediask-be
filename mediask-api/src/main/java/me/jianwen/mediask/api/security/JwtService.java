package me.jianwen.mediask.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.config.JwtProperties;
import me.jianwen.mediask.dal.enums.UserTypeEnum;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * JWT 生成与解析服务
 */
@Component
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_USER_TYPE = "userType";
    private static final String CLAIM_PERMS = "perms";
    private static final String CLAIM_TOKEN_KIND = "tokenKind";

    public enum TokenKind {
        ACCESS,
        REFRESH
    }

    /**
     * 生成用户访问令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param userType 用户类型
     * @return token 及过期时间
     */
    public JwtToken generateToken(Long userId, String username, UserTypeEnum userType, List<String> authorities) {
        return generateAccessToken(userId, username, userType, authorities);
    }

    public JwtToken generateAccessToken(Long userId, String username, UserTypeEnum userType, List<String> authorities) {
        return buildToken(userId, username, userType, authorities, TokenKind.ACCESS, properties.getExpireSeconds());
    }

    public JwtToken generateRefreshToken(Long userId, String username, UserTypeEnum userType, List<String> authorities) {
        return buildToken(userId, username, userType, authorities, TokenKind.REFRESH, properties.getRefreshExpireSeconds());
    }

    private JwtToken buildToken(
            Long userId,
            String username,
            UserTypeEnum userType,
            List<String> authorities,
            TokenKind tokenKind,
            long expireSeconds
    ) {
        properties.validate();

        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        var signingKey = Keys.hmacShaKeyFor(keyBytes);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_USER_TYPE, userType != null ? userType.getCode() : null)
                .claim(CLAIM_PERMS, authorities)
                .claim(CLAIM_TOKEN_KIND, tokenKind.name())
                // jjwt 0.12+: SignatureAlgorithm.HS256 已过时，使用 Jwts.SIG.HS256
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new JwtToken(token, expireAt.getEpochSecond());
    }

    /**
     * 解析并验证 token
     */
    public JwtPayload parseToken(String token) {
        try {
            properties.validate();
            byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
            var signingKey = Keys.hmacShaKeyFor(keyBytes);
            Claims claims = Jwts.parser()
                    // jjwt 0.12+: setSigningKey 已过时，使用 verifyWith
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Optional.ofNullable(claims.getSubject()).map(Long::valueOf).orElse(null);
            String username = claims.get(CLAIM_USERNAME, String.class);
            Integer userTypeCode = claims.get(CLAIM_USER_TYPE, Integer.class);
            List<String> perms = readStringListClaim(claims, CLAIM_PERMS);
            String tokenKindRaw = claims.get(CLAIM_TOKEN_KIND, String.class);
            TokenKind tokenKind = parseTokenKindOrDefault(tokenKindRaw);

            return new JwtPayload(userId, username, userTypeCode, perms, tokenKind);
        } catch (JwtException ex) {
            // 统一抛出，交由上层映射为业务错误码
            throw ex;
        }
    }

    private static List<String> readStringListClaim(Claims claims, String key) {
        Object raw = claims.get(key);
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

    private TokenKind parseTokenKindOrDefault(String raw) {
        if (raw == null || raw.isBlank()) {
            // 兼容历史 token（无 tokenKind claim）
            return TokenKind.ACCESS;
        }
        try {
            return TokenKind.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return TokenKind.ACCESS;
        }
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
    public record JwtPayload(Long userId, String username, Integer userType, List<String> authorities, TokenKind tokenKind) {
    }
}
