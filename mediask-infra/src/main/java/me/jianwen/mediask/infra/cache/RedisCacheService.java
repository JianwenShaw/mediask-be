package me.jianwen.mediask.infra.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.domain.cache.CacheService;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的缓存服务实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheService implements CacheService {

    private static final long SCAN_BATCH_SIZE = 500L;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        stringRedisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key));
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public long deleteByPattern(String pattern) {
        return Optional.ofNullable(stringRedisTemplate.execute((RedisConnection connection) -> {
            long deleted = 0L;
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SCAN_BATCH_SIZE).build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    byte[] rawKey = cursor.next();
                    Long result = connection.del(rawKey);
                    if (result != null) {
                        deleted += result;
                    }
                }
            } catch (Exception exception) {
                log.error("Delete keys by pattern failed, pattern={}", pattern, exception);
            }
            return deleted;
        })).orElse(0L);
    }

    @Override
    public Optional<Long> ttl(String key) {
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return (ttl != null && ttl > 0) ? Optional.of(ttl) : Optional.empty();
    }
}
