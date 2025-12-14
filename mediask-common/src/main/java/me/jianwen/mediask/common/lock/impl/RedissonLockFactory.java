package me.jianwen.mediask.common.lock.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.lock.DistributedLock;
import me.jianwen.mediask.common.lock.DistributedLockFactory;
import me.jianwen.mediask.common.lock.config.DistributedLockProperties;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 锁工厂实现
 * <p>
 * 职责：基于 Redisson 创建各种类型的分布式锁
 * 支持的锁类型：
 * 1. 可重入锁（默认）
 * 2. 公平锁（FIFO）
 * 3. 读写锁（ReadWriteLock）
 * 4. 红锁（RedLock，多 Redis 实例）
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonLockFactory implements DistributedLockFactory {

    private final RedissonClient redissonClient;
    private final DistributedLockProperties properties;

    @Override
    public DistributedLock createLock(String lockKey) {
        String fullKey = buildFullKey(lockKey);
        RLock rLock = redissonClient.getLock(fullKey);
        log.debug("创建分布式锁: lockKey={}", fullKey);
        return new RedissonDistributedLock(rLock, fullKey, properties.getDefaultLeaseTime());
    }

    @Override
    public DistributedLock createLock(String lockKey, long defaultLeaseTime, TimeUnit unit) {
        String fullKey = buildFullKey(lockKey);
        RLock rLock = redissonClient.getLock(fullKey);
        long leaseTimeSeconds = unit.toSeconds(defaultLeaseTime);
        log.debug("创建分布式锁（自定义租约）: lockKey={}, leaseTime={}s", fullKey, leaseTimeSeconds);
        return new RedissonDistributedLock(rLock, fullKey, leaseTimeSeconds);
    }

    @Override
    public DistributedLock createFairLock(String lockKey) {
        String fullKey = buildFullKey(lockKey);
        RLock fairLock = redissonClient.getFairLock(fullKey);
        log.debug("创建公平锁: lockKey={}", fullKey);
        return new RedissonDistributedLock(fairLock, fullKey, properties.getDefaultLeaseTime());
    }

    @Override
    public DistributedLock createReadLock(String lockKey) {
        String fullKey = buildFullKey(lockKey);
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(fullKey);
        RLock readLock = readWriteLock.readLock();
        log.debug("创建读锁: lockKey={}", fullKey);
        return new RedissonDistributedLock(readLock, fullKey + ":read", properties.getDefaultLeaseTime());
    }

    @Override
    public DistributedLock createWriteLock(String lockKey) {
        String fullKey = buildFullKey(lockKey);
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(fullKey);
        RLock writeLock = readWriteLock.writeLock();
        log.debug("创建写锁: lockKey={}", fullKey);
        return new RedissonDistributedLock(writeLock, fullKey + ":write", properties.getDefaultLeaseTime());
    }

    /**
     * 构建完整锁键（添加前缀）
     *
     * @param lockKey 业务锁键
     * @return 完整锁键
     */
    private String buildFullKey(String lockKey) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            throw new IllegalArgumentException("lockKey 不能为空");
        }
        return properties.getKeyPrefix() + lockKey;
    }
}
