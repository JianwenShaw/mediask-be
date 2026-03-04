package me.jianwen.mediask.infra.cache;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import me.jianwen.mediask.domain.cache.NullValue;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存写入协调器。
 *
 * <p>负责将数据回填到各缓存层（L1 / L2），统一管理：
 * <ul>
 *   <li>正常值回填：按 {@link CacheDefinition} 层级写入 L2 (Redis) + L1 (Caffeine)</li>
 *   <li>空值回填：防止缓存穿透（{@link NullValue}）</li>
 *   <li>TTL 随机偏移：±10% 抖动防止缓存雪崩</li>
 *   <li>显式写入：外部主动 {@code put} 并触发跨实例 L1 失效广播</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
class CacheWriteCoordinator {

    /**
     * TTL 随机偏移范围（±10%），防止大量 Key 同时过期导致缓存雪崩
     */
    private static final double TTL_JITTER_RATIO = 0.1;

    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisCacheAdapter redisCacheAdapter;
    private final CacheInvalidationBus invalidationBus;

    /**
     * 显式写入缓存。
     *
     * <p>写入 L2（带 TTL 抖动）→ 写入 L1 → 广播失效消息给其他实例。
     *
     * @param definition 缓存定义
     * @param fullKey    完整缓存键（已由 {@link CacheKeyGenerator} 生成）
     * @param value      缓存值
     */
    <T> void put(CacheDefinition definition, String fullKey, T value) {
        if (definition.hasRemoteLevel()) {
            Duration jitteredTtl = applyJitter(definition.remoteTtl());
            redisCacheAdapter.put(fullKey, value, jitteredTtl);
        }

        if (definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, value);
        }

        // 广播失效（其他实例的 L1 需要清除旧值，让它们下次读取时从 L2 获取最新值）
        if (definition.hasLocalLevel()) {
            invalidationBus.publish(definition.name(), fullKey);
        }
    }

    /**
     * 回填正常值到所有缓存层（内部回源后调用，不触发广播）。
     */
    <T> void backfill(CacheDefinition definition, String fullKey, T value) {
        if (definition.hasRemoteLevel()) {
            Duration jitteredTtl = applyJitter(definition.remoteTtl());
            redisCacheAdapter.put(fullKey, value, jitteredTtl);
        }
        if (definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, value);
        }
    }

    /**
     * 回填空值占位符到所有缓存层（防穿透）。
     */
    void backfillNull(CacheDefinition definition, String fullKey) {
        if (definition.hasRemoteLevel()) {
            Duration nullTtl = applyJitter(definition.nullValueTtl());
            redisCacheAdapter.putRaw(fullKey, NullValue.SERIALIZED_MARKER, nullTtl);
        }
        if (definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, NullValue.INSTANCE);
        }
    }

    /**
     * 对 TTL 施加 ±10% 随机偏移，防止大量 Key 同时过期引发缓存雪崩。
     *
     * <p>例如原始 TTL 为 60 分钟，偏移后范围为 54~66 分钟。
     *
     * @param baseTtl 原始 TTL
     * @return 施加抖动后的 TTL
     */
    Duration applyJitter(Duration baseTtl) {
        long millis = baseTtl.toMillis();
        long jitter = (long) (millis * TTL_JITTER_RATIO);
        if (jitter == 0) {
            return baseTtl;
        }
        long offset = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        return Duration.ofMillis(millis + offset);
    }
}
