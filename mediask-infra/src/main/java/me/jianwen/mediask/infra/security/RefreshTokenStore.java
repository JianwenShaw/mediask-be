package me.jianwen.mediask.infra.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.infra.cache.RedisCacheAdapter;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 存储服务（单设备模型）。
 *
 * <p>每个用户在 Redis 中仅保留一个 Refresh Token。新登录会覆盖旧 Token，
 * 自动踢掉前一次会话，确保同一用户同时只有一个有效的 Refresh Token。
 *
 * <p>使用 {@link RedisCacheAdapter} 直接操作 Redis（REMOTE only），实现：
 * <ol>
 *   <li>登录/注册时存储 Token（覆盖写，踢掉旧会话）</li>
 *   <li>登出时撤销 Token</li>
 *   <li>刷新时原子轮换 Token</li>
 *   <li>校验 Token 是否为当前有效会话</li>
 * </ol>
 *
 * <h3>键结构</h3>
 * <ul>
 *   <li>{@code mediask:auth:refresh:{userId}} — value 为当前有效的 tokenId</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenStore {

    /**
     * Token 键前缀（带全局命名空间）
     */
    private static final String KEY_PREFIX = "mediask:auth:refresh:";

    private static final Duration DEFAULT_TTL = Duration.ofDays(30);

    private final RedisCacheAdapter redisCacheAdapter;

    /**
     * 存储 Refresh Token（覆盖写，踢掉旧会话）。
     *
     * @param userId        用户ID
     * @param tokenId       Token ID (jti)
     * @param expireSeconds 过期时间（秒）
     */
    public void store(Long userId, String tokenId, long expireSeconds) {
        String key = tokenKey(userId);
        Duration ttl = expireSeconds > 0 ? Duration.ofSeconds(expireSeconds) : DEFAULT_TTL;
        redisCacheAdapter.putRaw(key, tokenId, ttl);
        log.debug("Refresh token stored: userId={}, tokenId={}", userId, tokenId);
    }

    /**
     * 删除 Refresh Token（登出时调用）。
     *
     * @param userId 用户ID
     */
    public void remove(Long userId) {
        String key = tokenKey(userId);
        if (redisCacheAdapter.delete(key)) {
            log.info("Refresh token revoked: userId={}", userId);
        }
    }

    /**
     * 检查 Refresh Token 是否为当前有效会话。
     *
     * <p>仅当 Redis 中存储的 tokenId 与传入的 tokenId 完全一致时才返回 true。
     * 旧会话的 tokenId 由于被覆盖写，自然失效。
     *
     * @param userId  用户ID
     * @param tokenId Token ID (jti)
     * @return 是否有效
     */
    public boolean isValid(Long userId, String tokenId) {
        String key = tokenKey(userId);
        String storedTokenId = redisCacheAdapter.getRaw(key);
        return tokenId.equals(storedTokenId);
    }

    /**
     * 生成用户维度的 Token 存储键。
     *
     * <p>格式：{@code mediask:auth:refresh:{userId}}
     */
    private static String tokenKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
