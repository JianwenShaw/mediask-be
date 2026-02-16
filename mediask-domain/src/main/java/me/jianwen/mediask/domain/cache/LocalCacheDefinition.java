package me.jianwen.mediask.domain.cache;

import java.time.Duration;

/**
 * 本地缓存项定义
 */
public record LocalCacheDefinition(
        String name,
        Duration expireAfterWrite,
        long maximumSize
) {
}
