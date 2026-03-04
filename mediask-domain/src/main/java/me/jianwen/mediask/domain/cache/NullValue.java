package me.jianwen.mediask.domain.cache;

import java.io.Serializable;

/**
 * 缓存空值占位符。
 *
 * <p>当 {@link CacheDefinition#cacheNullValues()} 启用时，回源加载返回 null 会用此对象占位，
 * 防止缓存穿透（不存在的 Key 反复查询数据库）。
 *
 * <p>设计要点：
 * <ul>
 *   <li>使用单例模式，避免重复创建</li>
 *   <li>实现 {@link Serializable}，支持跨层传输</li>
 *   <li>序列化为固定标记字符串 {@code "__NULL__"}，与真实业务数据区分</li>
 * </ul>
 */
public final class NullValue implements Serializable {

    /**
     * 全局唯一实例
     */
    public static final NullValue INSTANCE = new NullValue();

    /**
     * 序列化标记，在 Redis 中存储时使用此字符串代替 null
     */
    public static final String SERIALIZED_MARKER = "__NULL__";

    private NullValue() {
    }

    @Override
    public String toString() {
        return SERIALIZED_MARKER;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullValue;
    }

    @Override
    public int hashCode() {
        return NullValue.class.hashCode();
    }
}
