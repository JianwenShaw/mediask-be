package me.jianwen.mediask.domain.ratelimit;

import java.time.Duration;

/**
 * 分布式限流服务抽象
 */
public interface RateLimiterService {

    /**
     * 尝试获取限流许可
     *
     * @param key 限流 Key
     * @param permits 本次请求许可数
     * @param rateLimit 周期内允许的许可总数
     * @param rateInterval 限流周期
     * @return 是否获取成功
     */
    boolean tryAcquire(String key, long permits, long rateLimit, Duration rateInterval);
}
