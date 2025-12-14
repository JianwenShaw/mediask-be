package me.jianwen.mediask.common.lock.impl;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.lock.DistributedLock;
import me.jianwen.mediask.common.lock.exception.LockException;
import org.redisson.api.RLock;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁实现
 * <p>
 * 职责：基于 Redisson 实现分布式锁接口
 * 特性：
 * 1. 支持可重入（同一线程可多次获取同一把锁）
 * 2. 看门狗机制（自动续期，防止业务执行时间过长导致锁提前释放）
 * 3. Lua 脚本保证原子性（获取锁和释放锁都是原子操作）
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
@Slf4j
public class RedissonDistributedLock implements DistributedLock {

    /**
     * Redisson 锁实例
     */
    private final RLock rLock;

    /**
     * 锁键全名（包含前缀）
     */
    private final String lockKey;

    /**
     * 默认租约时间（秒）
     * -1 表示启用看门狗机制（自动续期）
     */
    private final long defaultLeaseTime;

    public RedissonDistributedLock(RLock rLock, String lockKey) {
        this(rLock, lockKey, -1);
    }

    public RedissonDistributedLock(RLock rLock, String lockKey, long defaultLeaseTime) {
        this.rLock = rLock;
        this.lockKey = lockKey;
        this.defaultLeaseTime = defaultLeaseTime;
    }

    @Override
    public boolean tryLock() {
        try {
            boolean acquired = rLock.tryLock();
            if (acquired) {
                log.debug("获取分布式锁成功: lockKey={}", lockKey);
            } else {
                log.debug("获取分布式锁失败（锁已被占用）: lockKey={}", lockKey);
            }
            return acquired;
        } catch (Exception e) {
            log.error("获取分布式锁异常: lockKey={}", lockKey, e);
            throw new LockException("获取锁失败: " + lockKey, e);
        }
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) {
        return tryLock(timeout, defaultLeaseTime, unit);
    }

    @Override
    public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) {
        try {
            boolean acquired = rLock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("获取分布式锁成功: lockKey={}, waitTime={}ms, leaseTime={}s",
                        lockKey, unit.toMillis(waitTime), unit.toSeconds(leaseTime));
            } else {
                log.warn("获取分布式锁超时: lockKey={}, waitTime={}s",
                        lockKey, unit.toSeconds(waitTime));
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断: lockKey={}", lockKey, e);
            throw new LockException("获取锁被中断: " + lockKey, e);
        } catch (Exception e) {
            log.error("获取分布式锁异常: lockKey={}", lockKey, e);
            throw new LockException("获取锁失败: " + lockKey, e);
        }
    }

    @Override
    public void unlock() {
        try {
            // 只有持有锁的线程才能释放
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
                log.debug("释放分布式锁成功: lockKey={}", lockKey);
            } else {
                log.warn("尝试释放未持有的锁: lockKey={}, currentThread={}",
                        lockKey, Thread.currentThread().getName());
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: lockKey={}", lockKey, e);
            // 释放锁失败不抛异常，避免影响业务流程
            // 锁会在 leaseTime 到期后自动释放
        }
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return rLock.isHeldByCurrentThread();
    }

    @Override
    public String getLockKey() {
        return lockKey;
    }

    @Override
    public void close() {
        unlock();
    }
}
