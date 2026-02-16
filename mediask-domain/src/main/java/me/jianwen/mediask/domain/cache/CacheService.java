package me.jianwen.mediask.domain.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * 缓存服务抽象
 *
 * <p>领域/应用层只依赖该接口，不直接依赖具体缓存客户端。
 */
public interface CacheService {

    /**
     * 写入缓存（不过期）
     *
     * @param key 缓存 Key
     * @param value 缓存值（字符串）
     */
    void set(String key, String value);

    /**
     * 写入缓存（带 TTL）
     *
     * @param key 缓存 Key
     * @param value 缓存值（字符串）
     * @param ttl 过期时间
     */
    void set(String key, String value, Duration ttl);

    /**
     * 获取缓存值
     *
     * @param key 缓存 Key
     * @return 缓存值，不存在返回 Optional.empty()
     */
    Optional<String> get(String key);

    /**
     * 删除缓存
     *
     * @param key 缓存 Key
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 判断 Key 是否存在
     *
     * @param key 缓存 Key
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 批量删除匹配模式的 Key
     *
     * @param pattern Key 模式（例如 auth:refresh:1:*）
     * @return 删除数量
     */
    long deleteByPattern(String pattern);

    /**
     * 获取 Key 剩余 TTL（秒）
     *
     * @param key 缓存 Key
     * @return 剩余秒数，不存在或已过期返回 Optional.empty()
     */
    Optional<Long> ttl(String key);
}
