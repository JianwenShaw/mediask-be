package me.jianwen.mediask.common.lock.exception;

/**
 * 获取锁超时异常
 * <p>
 * 场景：等待获取锁超过设定的 waitTime 时间后抛出
 * 处理建议：
 * 1. 业务层返回 "系统繁忙，请稍后重试"
 * 2. 记录日志，分析锁竞争情况
 * 3. 考虑调整 waitTime 或优化锁粒度
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
public class LockTimeoutException extends LockException {

    private final String lockKey;
    private final long timeoutMs;

    public LockTimeoutException(String lockKey, long timeoutMs) {
        super(String.format("获取分布式锁超时: lockKey=%s, timeout=%dms", lockKey, timeoutMs));
        this.lockKey = lockKey;
        this.timeoutMs = timeoutMs;
    }

    public LockTimeoutException(String lockKey, long timeoutMs, Throwable cause) {
        super(String.format("获取分布式锁超时: lockKey=%s, timeout=%dms", lockKey, timeoutMs), cause);
        this.lockKey = lockKey;
        this.timeoutMs = timeoutMs;
    }

    public String getLockKey() {
        return lockKey;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }
}
