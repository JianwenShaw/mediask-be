package me.jianwen.mediask.infra.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.domain.ratelimit.RateLimiterService;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redisson 的分布式限流实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonRateLimiterService implements RateLimiterService {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryAcquire(String key, long permits, long rateLimit, Duration rateInterval) {
        if (rateLimit <= 0 || permits <= 0 || rateInterval == null || rateInterval.isZero() || rateInterval.isNegative()) {
            log.warn("Invalid rate limit arguments, key={}, permits={}, rateLimit={}, rateInterval={}",
                    key, permits, rateLimit, rateInterval);
            return false;
        }

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        long rateIntervalMillis = rateInterval.toMillis();
        boolean rateInitialized = rateLimiter.trySetRate(
                RateType.OVERALL,
                rateLimit,
                rateIntervalMillis,
                RateIntervalUnit.MILLISECONDS
        );

        if (rateInitialized) {
            log.debug("Initialized rate limiter, key={}, rateLimit={}, intervalMs={}", key, rateLimit, rateIntervalMillis);
        }

        return rateLimiter.tryAcquire(permits);
    }
}
