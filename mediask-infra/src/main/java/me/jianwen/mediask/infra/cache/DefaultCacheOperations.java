package me.jianwen.mediask.infra.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.LockKeys;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import me.jianwen.mediask.domain.cache.CacheOperations;
import me.jianwen.mediask.domain.cache.NullValue;
import me.jianwen.mediask.infra.lock.DistributedLock;
import me.jianwen.mediask.infra.lock.DistributedLockFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 统一缓存操作引擎（{@link CacheOperations} 的唯一实现）。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>多级缓存读取链路编排：L1 (Caffeine) → L2 (Redis) → Loader 回源</li>
 *   <li>缓存回填：Loader 结果自动写入 L2 + L1</li>
 *   <li>空值缓存：防止缓存穿透（{@link NullValue}）</li>
 *   <li>TTL 随机偏移：±10% 抖动防止缓存雪崩</li>
 *   <li>分布式锁防击穿：同一 Key 的回源加载串行化（{@link LockKeys#CACHE_REBUILD}）</li>
 *   <li>跨实例 L1 失效广播：通过 {@link CacheInvalidationBus} Pub/Sub</li>
 * </ul>
 *
 * <h3>读取链路（TWO_LEVEL 模式）</h3>
 * <pre>
 *   L1 (Caffeine) 命中 → 返回（NullValue 判断）
 *        ↓ 未命中
 *   L2 (Redis) 命中 → 回填 L1 → 返回（NullValue 判断）
 *        ↓ 未命中
 *   加分布式锁 → 二次检查 L2 → Loader 回源 → 回填 L2 + L1 → 返回
 * </pre>
 *
 * <h3>写入/失效链路</h3>
 * <pre>
 *   evict: 清除本实例 L1 → 清除 L2 → 广播失效消息 → 其他实例清除 L1
 *   put:   写入 L2（带 TTL 抖动）→ 写入本实例 L1 → 广播失效消息
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCacheOperations implements CacheOperations {

    /**
     * TTL 随机偏移范围（±10%），防止大量 Key 同时过期导致缓存雪崩
     */
    private static final double TTL_JITTER_RATIO = 0.1;

    /**
     * 缓存重建锁的等待时间（秒）
     */
    private static final long REBUILD_LOCK_WAIT_SECONDS = 1;

    /**
     * 缓存重建锁的租约时间（秒），使用看门狗自动续期
     */
    private static final long REBUILD_LOCK_LEASE_SECONDS = -1;

    private final CaffeineCacheManager caffeineCacheManager;
    private final RedisCacheAdapter redisCacheAdapter;
    private final CacheKeyGenerator keyGenerator;
    private final CacheSerialization serialization;
    private final CacheInvalidationBus invalidationBus;
    private final DistributedLockFactory lockFactory;

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

        // 写入 L2
        if (definition.hasRemoteLevel()) {
            Duration jitteredTtl = applyJitter(definition.remoteTtl());
            redisCacheAdapter.put(fullKey, value, jitteredTtl);
        }

        // 写入 L1
        if (definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, value);
        }

        // 广播失效（其他实例的 L1 需要清除旧值，让它们下次读取时从 L2 获取最新值）
        if (definition.hasLocalLevel()) {
            invalidationBus.publish(definition.name(), fullKey);
        }
    }

    // ==================== 删除操作 ====================

    @Override
    public void evict(CacheDefinition definition, String key) {
        String fullKey = keyGenerator.generate(definition, key);

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

    @Override
    public void evictAll(CacheDefinition definition) {
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
     * <p>读取链路：L1 → L2 → 分布式锁 → 二次检查 L2 → Loader 回源 → 回填。
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

        // ---- Step 3: 全部未命中，加分布式锁回源加载 ----
        return loadWithLock(definition, fullKey, remoteRead, loader);
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

    /**
     * 加分布式锁后回源加载，防止缓存击穿。
     *
     * <p>流程：
     * <ol>
     *   <li>尝试获取分布式锁（等待 1 秒，看门狗自动续期）</li>
     *   <li>获取锁成功后二次检查 L2（可能已被其他线程/实例回填）</li>
     *   <li>调用 Loader 回源加载</li>
     *   <li>回填 L2 + L1</li>
     * </ol>
     *
     * <p>获取锁失败时降级为直接调用 Loader（保证可用性优先于防重复加载）。
     */
    private <T> T loadWithLock(CacheDefinition definition, String fullKey,
                               Supplier<T> remoteRead, Supplier<T> loader) {
        String lockKey = LockKeys.CACHE_REBUILD.buildKey(fullKey);
        DistributedLock lock = lockFactory.createLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(REBUILD_LOCK_WAIT_SECONDS, REBUILD_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);

            if (acquired) {
                // 二次检查 L2（其他线程/实例可能已回填）
                if (definition.hasRemoteLevel()) {
                    String rawJson = redisCacheAdapter.getRaw(fullKey);
                    if (rawJson != null) {
                        return handleRemoteHit(definition, fullKey, rawJson, remoteRead);
                    }
                }
            } else {
                log.debug("缓存重建锁获取失败，降级为直接加载: key={}", fullKey);
            }

            // 调用 Loader 回源
            return executeLoaderAndBackfill(definition, fullKey, loader);

        } catch (Exception exception) {
            log.error("缓存加载异常，降级为直接调用 Loader: key={}", fullKey, exception);
            return loader.get();
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行 Loader 并回填所有缓存层。
     */
    private <T> T executeLoaderAndBackfill(CacheDefinition definition, String fullKey, Supplier<T> loader) {
        T value = loader.get();

        if (value != null) {
            // 正常值回填
            backfill(definition, fullKey, value);
        } else if (definition.cacheNullValues()) {
            // 空值回填（防穿透）
            backfillNull(definition, fullKey);
        }

        return value;
    }

    /**
     * 回填正常值到所有缓存层。
     */
    private <T> void backfill(CacheDefinition definition, String fullKey, T value) {
        if (definition.hasRemoteLevel()) {
            Duration jitteredTtl = applyJitter(definition.remoteTtl());
            redisCacheAdapter.put(fullKey, value, jitteredTtl);
        }
        if (definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, value);
        }
    }

    /**
     * 回填空值占位符到所有缓存层。
     */
    private void backfillNull(CacheDefinition definition, String fullKey) {
        if (definition.hasRemoteLevel()) {
            Duration nullTtl = applyJitter(definition.nullValueTtl());
            redisCacheAdapter.putRaw(fullKey, NullValue.SERIALIZED_MARKER, nullTtl);
        }
        if (definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, NullValue.INSTANCE);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 对 TTL 施加 ±10% 随机偏移，防止大量 Key 同时过期引发缓存雪崩。
     *
     * <p>例如原始 TTL 为 60 分钟，偏移后范围为 54~66 分钟。
     *
     * @param baseTtl 原始 TTL
     * @return 施加抖动后的 TTL
     */
    private Duration applyJitter(Duration baseTtl) {
        long millis = baseTtl.toMillis();
        long jitter = (long) (millis * TTL_JITTER_RATIO);
        if (jitter == 0) {
            return baseTtl;
        }
        long offset = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        return Duration.ofMillis(millis + offset);
    }
}
