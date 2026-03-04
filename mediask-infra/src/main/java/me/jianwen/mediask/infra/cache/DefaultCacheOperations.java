package me.jianwen.mediask.infra.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import me.jianwen.mediask.domain.cache.CacheOperations;
import me.jianwen.mediask.domain.cache.NullValue;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 统一缓存操作引擎（{@link CacheOperations} 的唯一实现）。
 *
 * <p>作为门面类编排多级缓存的读取链路，将写入、重建、失效等职责委托给内部协作组件：
 * <ul>
 *   <li>{@link CacheWriteCoordinator} — 回填、空值写入、TTL 抖动、显式 put</li>
 *   <li>{@link CacheRebuildGuard} — 分布式锁 + double-check + 回源加载 + LOCAL 分流</li>
 *   <li>{@link CacheInvalidationCoordinator} — 单键/批量失效 + 跨实例广播</li>
 * </ul>
 *
 * <h3>读取链路（TWO_LEVEL / REMOTE 模式）</h3>
 * <pre>
 *   L1 (Caffeine) 命中 → 返回（NullValue 判断）
 *        ↓ 未命中
 *   L2 (Redis) 命中 → 回填 L1 → 返回（NullValue 判断）
 *        ↓ 未命中
 *   加分布式锁 → 二次检查 L2 → Loader 回源 → 回填 L2 + L1 → 返回
 * </pre>
 *
 * <h3>读取链路（LOCAL 模式）</h3>
 * <pre>
 *   L1 (Caffeine) 命中 → 返回（NullValue 判断）
 *        ↓ 未命中
 *   Loader 回源 → 回填 L1 → 返回（无分布式锁，无 Redis 依赖）
 * </pre>
 *
 * <h3>写入/失效链路</h3>
 * <pre>
 *   evict: 清除本实例 L1 → 清除 L2 → 广播失效消息 → 其他实例清除 L1
 *   put:   写入 L2（带 TTL 抖动）→ 写入本实例 L1 → 广播失效消息
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class DefaultCacheOperations implements CacheOperations {

    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisCacheAdapter redisCacheAdapter;
    private final CacheKeyGenerator keyGenerator;
    private final CacheSerialization serialization;
    private final CacheWriteCoordinator writeCoordinator;
    private final CacheRebuildGuard rebuildGuard;
    private final CacheInvalidationCoordinator invalidationCoordinator;

    // ==================== 读取操作 ====================

    @Override
    public <T> T get(CacheDefinition definition, String key, Class<T> type, Supplier<T> loader) {
        String fullKey = keyGenerator.generate(definition, key);
        return doGet(definition, fullKey, () -> redisCacheAdapter.get(fullKey, type), loader);
    }

    @Override
    public <T> T get(CacheDefinition definition, String key, TypeReference<T> typeReference, Supplier<T> loader) {
        String fullKey = keyGenerator.generate(definition, key);
        return doGet(definition, fullKey, () -> redisCacheAdapter.get(fullKey, typeReference), loader);
    }

    // ==================== 写入操作 ====================

    @Override
    public <T> void put(CacheDefinition definition, String key, T value) {
        String fullKey = keyGenerator.generate(definition, key);
        writeCoordinator.put(definition, fullKey, value);
    }

    // ==================== 删除操作 ====================

    @Override
    public void evict(CacheDefinition definition, String key) {
        String fullKey = keyGenerator.generate(definition, key);
        invalidationCoordinator.evict(definition, fullKey);
    }

    @Override
    public void evictAll(CacheDefinition definition) {
        invalidationCoordinator.evictAll(definition);
    }

    // ==================== 查询操作 ====================

    @Override
    public <T> Optional<T> getIfPresent(CacheDefinition definition, String key, Class<T> type) {
        String fullKey = keyGenerator.generate(definition, key);

        // 先查 L1
        if (definition.hasLocalLevel()) {
            Object localValue = caffeineCacheManager.get(definition, fullKey);
            if (localValue != null) {
                if (localValue instanceof NullValue) {
                    return Optional.empty();
                }
                @SuppressWarnings("unchecked")
                T result = (T) localValue;
                return Optional.of(result);
            }
        }

        // 再查 L2
        if (definition.hasRemoteLevel()) {
            String rawJson = redisCacheAdapter.getRaw(fullKey);
            if (rawJson != null) {
                if (serialization.isNullMarker(rawJson)) {
                    return Optional.empty();
                }
                T value = serialization.deserialize(rawJson, type);
                // 回填 L1
                if (definition.hasLocalLevel() && value != null) {
                    caffeineCacheManager.put(definition, fullKey, value);
                }
                return Optional.ofNullable(value);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean exists(CacheDefinition definition, String key) {
        String fullKey = keyGenerator.generate(definition, key);

        // L1 命中即返回（NullValue 也算存在）
        if (definition.hasLocalLevel()) {
            Object localValue = caffeineCacheManager.get(definition, fullKey);
            if (localValue != null) {
                return true;
            }
        }

        // L2 检查
        if (definition.hasRemoteLevel()) {
            return redisCacheAdapter.exists(fullKey);
        }

        return false;
    }

    // ==================== 核心读取链路 ====================

    /**
     * 多级缓存读取核心逻辑。
     *
     * <p>读取链路：L1 → L2 → {@link CacheRebuildGuard} 回源重建。
     *
     * @param definition  缓存定义
     * @param fullKey     完整缓存键
     * @param remoteRead  远程读取函数（Class / TypeReference 两种反序列化方式）
     * @param loader      回源加载器
     * @param <T>         值类型
     * @return 缓存值或回源结果
     */
    private <T> T doGet(CacheDefinition definition, String fullKey, Supplier<T> remoteRead, Supplier<T> loader) {
        // ---- Step 1: L1 查询 ----
        if (definition.hasLocalLevel()) {
            Object localValue = caffeineCacheManager.get(definition, fullKey);
            if (localValue != null) {
                return resolveLocalValue(localValue);
            }
        }

        // ---- Step 2: L2 查询 ----
        if (definition.hasRemoteLevel()) {
            String rawJson = redisCacheAdapter.getRaw(fullKey);
            if (rawJson != null) {
                return handleRemoteHit(definition, fullKey, rawJson, remoteRead);
            }
        }

        // ---- Step 3: 全部未命中，委托 RebuildGuard 回源加载 ----
        return rebuildGuard.rebuild(definition, fullKey, remoteRead, loader);
    }

    /**
     * 解析 L1 命中的值。
     *
     * <p>如果是 {@link NullValue} 占位符则返回 null，否则返回真实值。
     */
    @SuppressWarnings("unchecked")
    private <T> T resolveLocalValue(Object localValue) {
        if (localValue instanceof NullValue) {
            return null;
        }
        return (T) localValue;
    }

    /**
     * 处理 L2 命中的情况：反序列化 + 回填 L1。
     */
    private <T> T handleRemoteHit(CacheDefinition definition, String fullKey,
                                  String rawJson, Supplier<T> remoteRead) {
        // 空值标记
        if (serialization.isNullMarker(rawJson)) {
            if (definition.hasLocalLevel()) {
                caffeineCacheManager.put(definition, fullKey, NullValue.INSTANCE);
            }
            return null;
        }

        // 正常反序列化
        T value = remoteRead.get();
        if (value != null && definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, value);
        }
        return value;
    }
}
