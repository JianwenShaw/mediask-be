package me.jianwen.mediask.common.lock;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁统一接口
 * <p>
 * 职责：定义锁操作的标准契约，屏蔽底层实现差异（Redisson/Curator/自研）
 * 设计原则：
 * 1. 实现 AutoCloseable 支持 try-with-resources 自动释放
 * 2. 所有方法幂等，重复调用不会产生副作用
 * 3. 异常安全，确保锁一定能被释放
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
public interface DistributedLock extends AutoCloseable {

    /**
     * 尝试获取锁（非阻塞）
     * <p>
     * 场景：快速失败场景，如秒杀、抢购
     * </p>
     *
     * @return true=成功获取, false=锁已被占用
     */
    boolean tryLock();

    /**
     * 尝试获取锁（超时等待）
     * <p>
     * 场景：允许短暂等待的场景，如库存扣减
     * </p>
     *
     * @param timeout 等待时长
     * @param unit    时间单位
     * @return true=成功获取, false=超时未获取
     */
    boolean tryLock(long timeout, TimeUnit unit);

    /**
     * 尝试获取锁（完整参数）
     * <p>
     * 场景：需要精确控制等待时间和锁自动释放时间的场景
     * </p>
     *
     * @param waitTime  等待时长（获取锁的最大等待时间）
     * @param leaseTime 锁自动释放时间（防止死锁，看门狗机制）
     * @param unit      时间单位
     * @return true=成功获取, false=超时未获取
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit);

    /**
     * 释放锁（幂等操作）
     * <p>
     * 注意：只有持有锁的线程才能释放，其他线程调用无效
     * </p>
     */
    void unlock();

    /**
     * 是否持有锁
     * <p>
     * 用于判断当前线程是否持有该锁，避免非法释放
     * </p>
     *
     * @return true=当前线程持有锁, false=未持有
     */
    boolean isHeldByCurrentThread();

    /**
     * 获取锁键名（用于调试）
     * <p>
     * 返回完整的锁键（包含前缀），便于日志追踪和问题排查
     * </p>
     *
     * @return 锁键全名
     */
    String getLockKey();

    /**
     * 关闭锁（实现 AutoCloseable 接口）
     * <p>
     * 支持 try-with-resources 语法自动释放锁
     * </p>
     */
    @Override
    default void close() {
        unlock();
    }
}
