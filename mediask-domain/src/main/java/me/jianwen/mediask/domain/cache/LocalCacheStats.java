package me.jianwen.mediask.domain.cache;

/**
 * 本地缓存统计快照
 */
public record LocalCacheStats(
        long size,
        long hitCount,
        long missCount,
        double hitRate,
        long evictionCount
) {
}
