package me.jianwen.mediask.infra.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 远程缓存适配器。
 *
 * <p>封装 {@link StringRedisTemplate} 的缓存操作，提供：
 * <ul>
 *   <li>类型安全的读写（自动序列化/反序列化）</li>
 *   <li>批量删除优化（SCAN + UNLINK 替代逐个 DEL）</li>
 *   <li>原子递增（用于版本号等场景，避免 GET-then-SET 竞态）</li>
 * </ul>
 *
 * <p>本类仅负责 Redis 层的 CRUD，不涉及本地缓存或两级缓存编排。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheAdapter {

    /**
     * SCAN 命令每批扫描的 Key 数量（平衡性能与内存）
     */
    private static final long SCAN_BATCH_SIZE = 500L;

    /**
     * 批量 UNLINK 的阈值（累积到此数量后执行一次 UNLINK）
     */
    private static final int UNLINK_BATCH_SIZE = 500;

    private final StringRedisTemplate stringRedisTemplate;
    private final CacheSerialization serialization;

    /**
     * 读取缓存值。
     *
     * @param key  完整 Redis 键
     * @param type 值类型
     * @param <T>  值类型
     * @return 缓存值，不存在返回 null
     */
    public <T> T get(String key, Class<T> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        return serialization.deserialize(json, type);
    }

    /**
     * 读取缓存值（泛型类型）。
     *
     * @param key           完整 Redis 键
     * @param typeReference 类型引用
     * @param <T>           值类型
     * @return 缓存值，不存在返回 null
     */
    public <T> T get(String key, TypeReference<T> typeReference) {
        String json = stringRedisTemplate.opsForValue().get(key);
        return serialization.deserialize(json, typeReference);
    }

    /**
     * 读取原始 JSON 字符串（用于判断空值标记等场景）。
     *
     * @param key 完整 Redis 键
     * @return 原始 JSON 字符串，不存在返回 null
     */
    public String getRaw(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 写入缓存值（带 TTL）。
     *
     * @param key   完整 Redis 键
     * @param value 缓存值
     * @param ttl   过期时间
     * @param <T>   值类型
     */
    public <T> void put(String key, T value, Duration ttl) {
        String json = serialization.serialize(value);
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            stringRedisTemplate.opsForValue().set(key, json, ttl);
        } else {
            stringRedisTemplate.opsForValue().set(key, json);
        }
    }

    /**
     * 写入缓存值（不带 TTL）。
     *
     * @param key   完整 Redis 键
     * @param value 缓存值
     * @param <T>   值类型
     */
    public <T> void put(String key, T value) {
        String json = serialization.serialize(value);
        stringRedisTemplate.opsForValue().set(key, json);
    }

    /**
     * 删除单个缓存键。
     *
     * @param key 完整 Redis 键
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    /**
     * 判断缓存键是否存在。
     *
     * @param key 完整 Redis 键
     * @return 是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 获取 Key 剩余 TTL（秒）。
     *
     * @param key 完整 Redis 键
     * @return 剩余秒数，Key 不存在或已过期返回 {@link Optional#empty()}
     */
    public Optional<Long> ttl(String key) {
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? Optional.of(ttl) : Optional.empty();
    }

    /**
     * 批量删除匹配模式的 Key。
     *
     * <p>使用 SCAN（非阻塞）扫描 + UNLINK（非阻塞删除）替代 KEYS + DEL，
     * 避免阻塞 Redis 主线程。
     *
     * @param pattern Key 模式（如 {@code mediask:c:user:*}）
     * @return 删除的 Key 数量
     */
    public long deleteByPattern(String pattern) {
        Long result = stringRedisTemplate.execute((RedisConnection connection) -> {
            long deleted = 0L;
            List<byte[]> batch = new ArrayList<>(UNLINK_BATCH_SIZE);
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(SCAN_BATCH_SIZE)
                    .build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= UNLINK_BATCH_SIZE) {
                        Long unlinkResult = connection.unlink(batch.toArray(byte[][]::new));
                        deleted += unlinkResult != null ? unlinkResult : 0L;
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    Long unlinkResult = connection.unlink(batch.toArray(byte[][]::new));
                    deleted += unlinkResult != null ? unlinkResult : 0L;
                }
            } catch (Exception exception) {
                log.error("批量删除缓存键失败, pattern={}", pattern, exception);
            }
            return deleted;
        });
        return result != null ? result : 0L;
    }

    /**
     * 原子递增（替代 GET-then-SET，避免竞态条件）。
     *
     * <p>典型场景：版本号递增。Redis INCR 命令保证原子性，
     * 多实例并发调用不会出现版本号回退。
     *
     * @param key 完整 Redis 键
     * @return 递增后的值
     */
    public long increment(String key) {
        Long result = stringRedisTemplate.opsForValue().increment(key);
        return result != null ? result : 1L;
    }

    /**
     * 写入原始字符串值（不经过序列化层）。
     *
     * @param key   完整 Redis 键
     * @param value 原始字符串值
     * @param ttl   过期时间
     */
    public void putRaw(String key, String value, Duration ttl) {
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            stringRedisTemplate.opsForValue().set(key, value, ttl);
        } else {
            stringRedisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 写入原始字符串值（不带 TTL）。
     *
     * @param key   完整 Redis 键
     * @param value 原始字符串值
     */
    public void putRaw(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

}
