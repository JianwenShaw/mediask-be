package me.jianwen.mediask.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.LockKeys;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import me.jianwen.mediask.infra.lock.DistributedLock;
import me.jianwen.mediask.infra.lock.DistributedLockFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存重建守卫。
 *
 * <p>负责在缓存全部未命中时协调回源加载，防止缓存击穿：
 * <ul>
 *   <li>REMOTE / TWO_LEVEL 模式：通过分布式锁（{@link LockKeys#CACHE_REBUILD}）串行化同一 Key 的回源</li>
 *   <li>LOCAL 模式：无需分布式锁，直接回源并回填本地缓存</li>
 *   <li>锁获取失败时降级为直接加载（可用性优先于防重复加载）</li>
 *   <li>异常时降级为无锁加载并尝试回填（保证系统自恢复命中率）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CacheRebuildGuard {

    /**
     * 缓存重建锁的等待时间（秒）
     */
    private static final long REBUILD_LOCK_WAIT_SECONDS = 1;

    /**
     * 缓存重建锁的租约时间（秒），使用看门狗自动续期
     */
    private static final long REBUILD_LOCK_LEASE_SECONDS = -1;

    private final RedisCacheAdapter redisCacheAdapter;
    private final CaffeineCacheManager caffeineCacheManager;
    private final CacheWriteCoordinator writeCoordinator;
    private final DistributedLockFactory lockFactory;
    private final CacheSerialization serialization;

    /**
     * 缓存全部未命中时的回源加载入口。
     *
     * <p>根据缓存层级自动分流：
     * <ul>
     *   <li>有远程层：走分布式锁路径 ({@link #loadWithLock})</li>
     *   <li>仅本地层：直接回源回填 ({@link #executeLoaderAndBackfill})</li>
     * </ul>
     *
     * @param definition 缓存定义
     * @param fullKey    完整缓存键
     * @param remoteRead 远程读取函数（用于锁获取后二次检查 L2）
     * @param loader     回源加载器
     * @param <T>        值类型
     * @return 加载结果
     */
    <T> T rebuild(CacheDefinition definition, String fullKey,
                  Supplier<T> remoteRead, Supplier<T> loader) {
        if (definition.hasRemoteLevel()) {
            return loadWithLock(definition, fullKey, remoteRead, loader);
        } else {
            return executeLoaderAndBackfill(definition, fullKey, loader);
        }
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
                String rawJson = redisCacheAdapter.getRaw(fullKey);
                if (rawJson != null) {
                    return handleRemoteHit(definition, fullKey, rawJson, remoteRead);
                }
            } else {
                log.debug("缓存重建锁获取失败，降级为直接加载: key={}", fullKey);
            }

            // 调用 Loader 回源
            return executeLoaderAndBackfill(definition, fullKey, loader);

        } catch (Exception exception) {
            log.error("缓存加载异常，降级为无锁加载并回填: key={}", fullKey, exception);
            return executeLoaderAndBackfill(definition, fullKey, loader);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行 Loader 并回填所有缓存层。
     */
    <T> T executeLoaderAndBackfill(CacheDefinition definition, String fullKey, Supplier<T> loader) {
        T value = loader.get();

        if (value != null) {
            writeCoordinator.backfill(definition, fullKey, value);
        } else if (definition.cacheNullValues()) {
            writeCoordinator.backfillNull(definition, fullKey);
        }

        return value;
    }

    /**
     * 处理 L2 命中的情况（重建锁获取后二次检查命中时使用）：反序列化 + 回填 L1。
     */
    private <T> T handleRemoteHit(CacheDefinition definition, String fullKey,
                                  String rawJson, Supplier<T> remoteRead) {
        if (serialization.isNullMarker(rawJson)) {
            if (definition.hasLocalLevel()) {
                caffeineCacheManager.put(definition, fullKey, me.jianwen.mediask.domain.cache.NullValue.INSTANCE);
            }
            return null;
        }

        T value = remoteRead.get();
        if (value != null && definition.hasLocalLevel()) {
            caffeineCacheManager.put(definition, fullKey, value);
        }
        return value;
    }
}
