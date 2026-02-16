package me.jianwen.mediask.domain.cache;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 本地缓存服务抽象
 */
public interface LocalCacheService {

    /**
     * 读取缓存，未命中时通过 loader 加载并回填
     *
     * @param definition 缓存定义
     * @param key 缓存键
     * @param loader 回源加载器
     * @return 缓存值
     * @param <T> 值类型
     */
    <T> T get(LocalCacheDefinition definition, String key, Supplier<T> loader);

    /**
     * 删除指定键
     */
    void invalidate(LocalCacheDefinition definition, String key);

    /**
     * 清空指定缓存
     */
    void invalidateAll(LocalCacheDefinition definition);

    /**
     * 获取缓存统计
     */
    LocalCacheStats stats(LocalCacheDefinition definition);

    /**
     * 获取所有缓存统计快照
     */
    Map<String, LocalCacheStats> allStats();
}
