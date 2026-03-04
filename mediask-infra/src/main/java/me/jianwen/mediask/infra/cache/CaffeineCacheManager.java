package me.jianwen.mediask.infra.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Caffeine 本地缓存管理器。
 *
 * <p>管理所有 {@link CacheDefinition} 对应的 Caffeine 缓存实例，
 * 按缓存名称（{@link CacheDefinition#name()}）懒初始化并复用。
 *
 * <p>本类仅负责本地缓存的 CRUD，不涉及远程缓存或跨实例失效逻辑。
 * 两级缓存的编排由 {@link DefaultCacheOperations} 统一调度。
 *
 * <h3>线程安全</h3>
 * <ul>
 *   <li>缓存实例通过 {@link ConcurrentHashMap#computeIfAbsent} 懒创建，保证只初始化一次</li>
 *   <li>Caffeine 本身是线程安全的</li>
 * </ul>
 */
@Slf4j
@Component
public class CaffeineCacheManager {

    /**
     * 缓存名称 → Caffeine 实例的映射
     */
    private final ConcurrentMap<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();

    /**
     * 已注册的缓存定义（用于检测同名不同配置的冲突）
     */
    private final ConcurrentMap<String, CacheDefinition> definitions = new ConcurrentHashMap<>();

    /**
     * 获取缓存值。
     *
     * @param definition 缓存定义
     * @param key        完整缓存键（由 {@link CacheKeyGenerator} 生成）
     * @return 缓存值，未命中返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(CacheDefinition definition, String key) {
        Cache<String, Object> cache = resolveCache(definition);
        return (T) cache.getIfPresent(key);
    }

    /**
     * 写入缓存值。
     *
     * @param definition 缓存定义
     * @param key        完整缓存键
     * @param value      缓存值（不能为 null，空值请使用 {@link me.jianwen.mediask.domain.cache.NullValue}）
     */
    public void put(CacheDefinition definition, String key, Object value) {
        Cache<String, Object> cache = resolveCache(definition);
        cache.put(key, value);
    }

    /**
     * 删除缓存值。
     *
     * @param definition 缓存定义
     * @param key        完整缓存键
     */
    public void evict(CacheDefinition definition, String key) {
        Cache<String, Object> cache = caches.get(definition.name());
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * 通过缓存名称删除缓存值（用于接收跨实例失效消息时，只有 cacheName 和 key）。
     *
     * @param cacheName 缓存名称
     * @param key       完整缓存键
     */
    public void evictByName(String cacheName, String key) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    /**
     * 清空指定缓存的所有条目。
     *
     * @param definition 缓存定义
     */
    public void evictAll(CacheDefinition definition) {
        Cache<String, Object> cache = caches.get(definition.name());
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /**
     * 通过缓存名称清空所有条目（用于接收跨实例失效广播）。
     *
     * @param cacheName 缓存名称
     */
    public void evictAllByName(String cacheName) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /**
     * 获取指定缓存的统计信息。
     *
     * @param definition 缓存定义
     * @return Caffeine 统计快照
     */
    public CaffeineStatsSnapshot stats(CacheDefinition definition) {
        Cache<String, Object> cache = caches.get(definition.name());
        if (cache == null) {
            return CaffeineStatsSnapshot.EMPTY;
        }
        var stats = cache.stats();
        return new CaffeineStatsSnapshot(
                cache.estimatedSize(),
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate(),
                stats.evictionCount()
        );
    }

    /**
     * 获取所有缓存的统计信息。
     */
    public Map<String, CaffeineStatsSnapshot> allStats() {
        return caches.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    var cache = entry.getValue();
                    var stats = cache.stats();
                    return new CaffeineStatsSnapshot(
                            cache.estimatedSize(),
                            stats.hitCount(),
                            stats.missCount(),
                            stats.hitRate(),
                            stats.evictionCount()
                    );
                }
        ));
    }

    /**
     * 解析或创建 Caffeine 缓存实例。
     *
     * <p>同名缓存只会创建一次。如果同名但配置不同（TTL / maxSize），抛出异常防止静默覆盖。
     */
    private Cache<String, Object> resolveCache(CacheDefinition definition) {
        CacheDefinition existing = definitions.putIfAbsent(definition.name(), definition);
        if (existing != null && hasDifferentLocalSpec(existing, definition)) {
            throw new IllegalArgumentException(
                    "缓存名称冲突：'" + definition.name() + "' 已存在且本地缓存配置不同。" +
                    " 已有: localTtl=" + existing.localTtl() + ", maxSize=" + existing.localMaxSize() +
                    " 新的: localTtl=" + definition.localTtl() + ", maxSize=" + definition.localMaxSize());
        }

        return caches.computeIfAbsent(definition.name(), name -> {
            log.info("初始化 Caffeine 本地缓存: name={}, expireAfterWrite={}, maximumSize={}",
                    definition.name(), definition.localTtl(), definition.localMaxSize());
            return Caffeine.newBuilder()
                    .recordStats()
                    .expireAfterWrite(definition.localTtl())
                    .maximumSize(definition.localMaxSize())
                    .build();
        });
    }

    private boolean hasDifferentLocalSpec(CacheDefinition a, CacheDefinition b) {
        return !a.localTtl().equals(b.localTtl()) || a.localMaxSize() != b.localMaxSize();
    }

    /**
     * Caffeine 统计快照
     */
    public record CaffeineStatsSnapshot(
            long size,
            long hitCount,
            long missCount,
            double hitRate,
            long evictionCount
    ) {
        public static final CaffeineStatsSnapshot EMPTY = new CaffeineStatsSnapshot(0, 0, 0, 0.0, 0);
    }
}
