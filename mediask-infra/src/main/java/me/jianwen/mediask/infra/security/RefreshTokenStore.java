package me.jianwen.mediask.infra.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.domain.cache.CacheService;
import me.jianwen.mediask.infra.cache.CacheKeyManager;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token 存储服务
 *
 * 用于存储用户 Refresh Token 到 Redis，实现：
 * 1. 登出时撤销 Refresh Token
 * 2. 检测 Refresh Token 是否已被撤销
 * 3. 支持多设备登录（每个设备一个 Refresh Token）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final CacheService cacheService;

    private static final String TOKEN_ACTIVE_FLAG = "1";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    /**
     * 存储 Refresh Token
     *
     * @param userId    用户ID
     * @param tokenId   Token ID (jti)
     * @param expireSeconds 过期时间（秒）
     */
    public void store(Long userId, String tokenId, long expireSeconds) {
        String key = CacheKeyManager.refreshTokenKey(userId, tokenId);
        Duration ttl = expireSeconds > 0 ? Duration.ofSeconds(expireSeconds) : DEFAULT_TTL;
        cacheService.set(key, TOKEN_ACTIVE_FLAG, ttl);
        log.debug("Refresh token stored: userId={}, tokenId={}", userId, tokenId);
    }

    /**
     * 删除 Refresh Token（登出时调用）
     *
     * @param userId  用户ID
     * @param tokenId Token ID (jti)
     */
    public void remove(Long userId, String tokenId) {
        String key = CacheKeyManager.refreshTokenKey(userId, tokenId);
        if (cacheService.delete(key)) {
            log.info("Refresh token revoked: userId={}, tokenId={}", userId, tokenId);
        }
    }

    /**
     * 检查 Refresh Token 是否有效
     *
     * @param userId  用户ID
     * @param tokenId Token ID (jti)
     * @return 是否有效
     */
    public boolean isValid(Long userId, String tokenId) {
        String key = CacheKeyManager.refreshTokenKey(userId, tokenId);
        return cacheService.exists(key);
    }

    /**
     * 撤销用户的所有 Refresh Token（登出所有设备）
     *
     * @param userId 用户ID
     */
    public void revokeAll(Long userId) {
        String pattern = CacheKeyManager.refreshTokenPattern(userId);
        long deletedCount = cacheService.deleteByPattern(pattern);
        if (deletedCount > 0) {
            log.info("All refresh tokens revoked for userId={}, count={}", userId, deletedCount);
        }
    }

    /**
     * 获取 Refresh Token 剩余 TTL（秒）
     *
     * @param userId  用户ID
     * @param tokenId Token ID (jti)
     * @return 剩余时间，Token 不存在返回 Optional.empty()
     */
    public Optional<Long> getTtl(Long userId, String tokenId) {
        String key = CacheKeyManager.refreshTokenKey(userId, tokenId);
        return cacheService.ttl(key);
    }
}
