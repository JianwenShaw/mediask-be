package me.jianwen.mediask.infra.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.infra.cache.RedisCacheAdapter;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token 存储服务。
 *
 * <p>使用 {@link RedisCacheAdapter} 直接操作 Redis（REMOTE only），实现：
 * <ol>
 *   <li>登出时撤销 Refresh Token</li>
 *   <li>检测 Refresh Token 是否已被撤销</li>
 *   <li>支持多设备登录（每个设备一个 Refresh Token）</li>
 * </ol>
 *
 * <p>Token 存储不走 {@code CacheOperations}，因为每个 Token 有独立的 TTL（与 JWT 过期时间对齐），
 * 不适合用统一的 {@code CacheDefinition} 声明固定 TTL。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenStore {

    /**
     * Token 键前缀（由 CacheKeyGenerator 统一前缀 mediask:c: 加上此前缀）
     */
    private static final String KEY_PREFIX = "auth:refresh:";

    private static final String TOKEN_ACTIVE_FLAG = "1";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private final RedisCacheAdapter redisCacheAdapter;

    /**
     * 存储 Refresh Token
     *
     * @param userId        用户ID
     * @param tokenId       Token ID (jti)
     * @param expireSeconds 过期时间（秒）
     */
    public void store(Long userId, String tokenId, long expireSeconds) {
        String key = tokenKey(userId, tokenId);
        Duration ttl = expireSeconds > 0 ? Duration.ofSeconds(expireSeconds) : DEFAULT_TTL;
        redisCacheAdapter.putRaw(key, TOKEN_ACTIVE_FLAG, ttl);
        log.debug("Refresh token stored: userId={}, tokenId={}", userId, tokenId);
    }

    /**
     * 删除 Refresh Token（登出时调用）
     *
     * @param userId  用户ID
     * @param tokenId Token ID (jti)
     */
    public void remove(Long userId, String tokenId) {
        String key = tokenKey(userId, tokenId);
        if (redisCacheAdapter.delete(key)) {
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
        String key = tokenKey(userId, tokenId);
        return redisCacheAdapter.exists(key);
    }

    /**
     * 撤销用户的所有 Refresh Token（登出所有设备）
     *
     * @param userId 用户ID
     */
    public void revokeAll(Long userId) {
        String pattern = KEY_PREFIX + userId + ":*";
        long deletedCount = redisCacheAdapter.deleteByPattern(pattern);
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
        String key = tokenKey(userId, tokenId);
        return redisCacheAdapter.ttl(key);
    }

    /**
     * 生成 Token 存储键。
     *
     * <p>格式：{@code auth:refresh:{userId}:{tokenId}}
     */
    private static String tokenKey(Long userId, String tokenId) {
        return KEY_PREFIX + userId + ":" + tokenId;
    }
}
