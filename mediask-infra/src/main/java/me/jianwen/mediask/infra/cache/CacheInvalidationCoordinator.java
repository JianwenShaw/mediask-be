package me.jianwen.mediask.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import org.springframework.stereotype.Component;

/**
 * 缓存失效协调器。
 *
 * <p>负责单键和批量缓存失效操作，统一管理：
 * <ul>
 *   <li>清除本实例 L1 (Caffeine)</li>
 *   <li>清除 L2 (Redis)</li>
 *   <li>通过 {@link CacheInvalidationBus} 广播失效消息到其他实例</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CacheInvalidationCoordinator {

    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisCacheAdapter redisCacheAdapter;
    private final CacheKeyGenerator keyGenerator;
    private final CacheInvalidationBus invalidationBus;

    /**
     * 清除单个缓存键。
     *
     * <p>流程：清除本实例 L1 → 清除 L2 → 广播失效消息。
     *
     * @param definition 缓存定义
     * @param fullKey    完整缓存键（已由 {@link CacheKeyGenerator} 生成）
     */
    void evict(CacheDefinition definition, String fullKey) {
        // 先清除本实例 L1
        if (definition.hasLocalLevel()) {
            caffeineCacheManager.evict(definition, fullKey);
        }

        // 再清除 L2
        if (definition.hasRemoteLevel()) {
            redisCacheAdapter.delete(fullKey);
        }

        // 广播失效消息到其他实例的 L1
        if (definition.hasLocalLevel()) {
            invalidationBus.publish(definition.name(), fullKey);
        }
    }

    /**
     * 清除指定缓存定义下的所有缓存键。
     *
     * <p>流程：清除本实例 L1 → SCAN + UNLINK 清除 L2 → 广播全量失效消息。
     *
     * @param definition 缓存定义
     */
    void evictAll(CacheDefinition definition) {
        // 先清除本实例 L1
        if (definition.hasLocalLevel()) {
            caffeineCacheManager.evictAll(definition);
        }

        // 再清除 L2（通过 SCAN + UNLINK）
        if (definition.hasRemoteLevel()) {
            String pattern = keyGenerator.generatePattern(definition);
            long deleted = redisCacheAdapter.deleteByPattern(pattern);
            log.debug("批量清除远程缓存: definition={}, deleted={}", definition.name(), deleted);
        }

        // 广播全量失效
        if (definition.hasLocalLevel()) {
            invalidationBus.publish(definition.name(), null);
        }
    }
}
