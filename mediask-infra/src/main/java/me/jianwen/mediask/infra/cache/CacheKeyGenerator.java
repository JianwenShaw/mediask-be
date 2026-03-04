package me.jianwen.mediask.infra.cache;

import me.jianwen.mediask.domain.cache.CacheDefinition;
import org.springframework.stereotype.Component;

/**
 * 统一缓存键生成器。
 *
 * <p>负责将业务缓存键（如 {@code "user:123"}）与全局前缀、缓存名称拼接为
 * Redis 中的完整键，确保不同模块、不同缓存之间的键命名空间隔离。
 *
 * <h3>键格式</h3>
 * <pre>
 *   {GLOBAL_PREFIX}{cacheName}:{businessKey}
 *   例如：mediask:c:user:123
 * </pre>
 *
 * <h3>命名空间隔离</h3>
 * <ul>
 *   <li>缓存键: {@code mediask:c:} — 本类负责</li>
 *   <li>分布式锁: {@code mediask:lock:} — DistributedLockProperties 负责</li>
 *   <li>限流键: {@code rate:limit:} — RateLimitKeyManager 负责</li>
 * </ul>
 */
@Component
public class CacheKeyGenerator {

    /**
     * 全局缓存键前缀，与锁键 {@code mediask:lock:} 等命名空间隔离
     */
    private static final String GLOBAL_PREFIX = "mediask:c:";

    /**
     * 键分隔符
     */
    private static final String DELIMITER = ":";

    /**
     * 生成完整缓存键。
     *
     * @param definition 缓存定义（提供缓存名称）
     * @param key        业务缓存键
     * @return 完整的 Redis 键，如 {@code mediask:c:user:123}
     */
    public String generate(CacheDefinition definition, String key) {
        return GLOBAL_PREFIX + definition.name() + DELIMITER + key;
    }

    /**
     * 生成通配符模式（用于批量删除）。
     *
     * @param definition 缓存定义
     * @return 通配符模式，如 {@code mediask:c:user:*}
     */
    public String generatePattern(CacheDefinition definition) {
        return GLOBAL_PREFIX + definition.name() + DELIMITER + "*";
    }
}
