package me.jianwen.mediask.common.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁工厂
 * <p>
 * 职责：统一创建和管理锁实例，支持多种实现切换
 * 设计模式：工厂模式 + 策略模式
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
public interface DistributedLockFactory {

    /**
     * 创建分布式锁（使用默认配置）
     * <p>
     * 自动添加锁键前缀（mediask:lock:）
     * </p>
     *
     * @param lockKey 锁键（业务标识，如 "appt:123"）
     * @return 锁实例
     */
    DistributedLock createLock(String lockKey);

    /**
     * 创建锁（自定义默认租约时间）
     * <p>
     * 用于需要长时间持有锁的场景（如数据同步、批量导入）
     * </p>
     *
     * @param lockKey          锁键
     * @param defaultLeaseTime 默认租约时间（看门狗续期时间）
     * @param unit             时间单位
     * @return 锁实例
     */
    DistributedLock createLock(String lockKey, long defaultLeaseTime, TimeUnit unit);

    /**
     * 创建公平锁（FIFO 顺序获取）
     * <p>
     * 场景：需要严格保证先到先得的场景（如预约排队）
     * 注意：性能略低于非公平锁
     * </p>
     *
     * @param lockKey 锁键
     * @return 公平锁实例
     */
    DistributedLock createFairLock(String lockKey);

    /**
     * 创建读写锁（读锁）
     * <p>
     * 场景：读多写少的场景，多个读操作可并发执行
     * </p>
     *
     * @param lockKey 锁键
     * @return 读锁实例
     */
    DistributedLock createReadLock(String lockKey);

    /**
     * 创建读写锁（写锁）
     * <p>
     * 场景：写操作时需要独占访问
     * </p>
     *
     * @param lockKey 锁键
     * @return 写锁实例
     */
    DistributedLock createWriteLock(String lockKey);
}
