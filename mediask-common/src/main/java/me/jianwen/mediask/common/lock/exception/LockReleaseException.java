package me.jianwen.mediask.common.lock.exception;

/**
 * 释放锁异常
 * <p>
 * 场景：
 * 1. 释放未持有的锁
 * 2. 锁已过期自动释放后尝试手动释放
 * 3. Redis 连接异常导致释放失败
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
public class LockReleaseException extends LockException {

    private final String lockKey;

    public LockReleaseException(String lockKey) {
        super(String.format("释放分布式锁失败: lockKey=%s", lockKey));
        this.lockKey = lockKey;
    }

    public LockReleaseException(String lockKey, Throwable cause) {
        super(String.format("释放分布式锁失败: lockKey=%s", lockKey), cause);
        this.lockKey = lockKey;
    }

    public LockReleaseException(String lockKey, String reason) {
        super(String.format("释放分布式锁失败: lockKey=%s, reason=%s", lockKey, reason));
        this.lockKey = lockKey;
    }

    public String getLockKey() {
        return lockKey;
    }
}
