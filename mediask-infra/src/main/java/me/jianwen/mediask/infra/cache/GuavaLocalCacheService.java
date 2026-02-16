package me.jianwen.mediask.infra.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.domain.cache.LocalCacheDefinition;
import me.jianwen.mediask.domain.cache.LocalCacheService;
import me.jianwen.mediask.domain.cache.LocalCacheStats;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Guava 的本地缓存实现
 */
@Slf4j
@Component
public class GuavaLocalCacheService implements LocalCacheService {

    private static final long EMPTY_STAT_VALUE = 0L;
    private static final double EMPTY_HIT_RATE = 0D;

    private final ConcurrentMap<String, Cache<String, Object>> cacheStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LocalCacheDefinition> definitionStore = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(LocalCacheDefinition definition, String key, Supplier<T> loader) {
        Cache<String, Object> cache = resolveCache(definition);
        try {
            Object value = cache.get(key, () -> Objects.requireNonNull(loader.get(), "Local cache does not support null value"));
            return (T) value;
        } catch (Exception exception) {
            throw new IllegalStateException("Load local cache failed: " + definition.name(), exception);
        }
    }

    @Override
    public void invalidate(LocalCacheDefinition definition, String key) {
        Cache<String, Object> cache = cacheStore.get(definition.name());
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    @Override
    public void invalidateAll(LocalCacheDefinition definition) {
        Cache<String, Object> cache = cacheStore.get(definition.name());
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    @Override
    public LocalCacheStats stats(LocalCacheDefinition definition) {
        Cache<String, Object> cache = cacheStore.get(definition.name());
        return toStats(cache);
    }

    @Override
    public Map<String, LocalCacheStats> allStats() {
        return cacheStore.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toStats(entry.getValue())));
    }

    private Cache<String, Object> resolveCache(LocalCacheDefinition definition) {
        validateDefinition(definition);
        LocalCacheDefinition existedDefinition = definitionStore.putIfAbsent(definition.name(), definition);
        if (existedDefinition != null && hasDifferentSpec(existedDefinition, definition)) {
            throw new IllegalArgumentException("Duplicated cache name with different spec: " + definition.name());
        }

        return cacheStore.computeIfAbsent(definition.name(), ignored -> {
            log.info("Initialize local cache: name={}, expireAfterWrite={}, maximumSize={}",
                    definition.name(), definition.expireAfterWrite(), definition.maximumSize());
            return CacheBuilder.newBuilder()
                    .recordStats()
                    .expireAfterWrite(definition.expireAfterWrite())
                    .maximumSize(definition.maximumSize())
                    .build();
        });
    }

    private boolean hasDifferentSpec(LocalCacheDefinition oldDefinition, LocalCacheDefinition newDefinition) {
        return !oldDefinition.expireAfterWrite().equals(newDefinition.expireAfterWrite())
                || oldDefinition.maximumSize() != newDefinition.maximumSize();
    }

    private void validateDefinition(LocalCacheDefinition definition) {
        if (definition == null || definition.name() == null || definition.name().isBlank()) {
            throw new IllegalArgumentException("Local cache definition name is required");
        }
        if (definition.expireAfterWrite() == null || definition.expireAfterWrite().isNegative() || definition.expireAfterWrite().isZero()) {
            throw new IllegalArgumentException("Local cache expireAfterWrite must be positive");
        }
        if (definition.maximumSize() <= 0) {
            throw new IllegalArgumentException("Local cache maximumSize must be positive");
        }
    }

    private LocalCacheStats toStats(Cache<String, Object> cache) {
        if (cache == null) {
            return new LocalCacheStats(EMPTY_STAT_VALUE, EMPTY_STAT_VALUE, EMPTY_STAT_VALUE, EMPTY_HIT_RATE, EMPTY_STAT_VALUE);
        }
        CacheStats cacheStats = cache.stats();
        return new LocalCacheStats(
                cache.size(),
                cacheStats.hitCount(),
                cacheStats.missCount(),
                cacheStats.hitRate(),
                cacheStats.evictionCount()
        );
    }
}
