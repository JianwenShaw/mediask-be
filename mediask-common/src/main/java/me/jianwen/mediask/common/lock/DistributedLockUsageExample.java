package me.jianwen.mediask.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.LockKeys;
import me.jianwen.mediask.common.lock.annotation.DistributedLockable;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁使用示例
 * <p>
 * 演示两种使用方式：
 * 1. 编程式锁（手动控制）
 * 2. 声明式锁（注解式 AOP）
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockUsageExample {

    private final DistributedLockFactory lockFactory;

    // ==================== 编程式锁（推荐：精细控制）====================

    /**
     * 示例1：try-with-resources + Enum 自动拼接（最推荐）
     */
    public void example1_EnumBuildKey(Long scheduleId) {
        // ✨ 使用 Enum 的 buildKey() 方法自动拼接
        String lockKey = LockKeys.APPT_CREATE.buildKey(scheduleId);

        try (DistributedLock lock = lockFactory.createLock(lockKey)) {
            // 使用枚举推荐的超时时间
            int waitTime = LockKeys.APPT_CREATE.getRecommendedWaitTime();
            int leaseTime = LockKeys.APPT_CREATE.getRecommendedLeaseTime();

            if (!lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS)) {
                log.warn("获取锁失败，系统繁忙: lockKey={}", lockKey);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            // 业务逻辑：扣减号源
            log.info("执行业务逻辑: scheduleId={}", scheduleId);
            decreaseSlots(scheduleId);

        } // 自动释放锁
    }

    /**
     * 示例2：兼容旧代码（使用 getPrefix）
     */
    public void example2_EnumPrefix(Long scheduleId) {
        // 兼容旧代码：使用 getPrefix()
        String lockKey = LockKeys.APPT_CREATE.getPrefix() + scheduleId;
        DistributedLock lock = lockFactory.createLock(lockKey);

        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new RuntimeException("系统繁忙");
            }

            // 业务逻辑
            decreaseSlots(scheduleId);

        } finally {
            lock.unlock(); // 手动释放
        }
    }

    /**
     * 示例3：公平锁（先到先得）
     */
    public void example3_FairLock(String userId) {
        String lockKey = LockKeys.USER_UPDATE.buildKey(userId);
        DistributedLock fairLock = lockFactory.createFairLock(lockKey);

        try {
            if (!fairLock.tryLock(5, 30, TimeUnit.SECONDS)) {
                throw new RuntimeException("系统繁忙");
            }

            // 业务逻辑：更新用户信息
            updateUserInfo(userId);

        } finally {
            fairLock.unlock();
        }
    }

    /**
     * 示例4：读写锁（读多写少场景）
     */
    public void example4_ReadWriteLock(String cacheKey) {
        // 读操作：多个线程可并发执行
        DistributedLock readLock = lockFactory.createReadLock(cacheKey);
        try {
            if (readLock.tryLock(1, TimeUnit.SECONDS)) {
                // 读取缓存
                String data = readCache(cacheKey);
                log.info("读取缓存: data={}", data);
            }
        } finally {
            readLock.unlock();
        }

        // 写操作：独占访问
        DistributedLock writeLock = lockFactory.createWriteLock(cacheKey);
        try {
            if (writeLock.tryLock(3, TimeUnit.SECONDS)) {
                // 更新缓存
                writeCache(cacheKey, "new data");
            }
        } finally {
            writeLock.unlock();
        }
    }

    // ==================== 声明式锁（推荐：简化代码）====================

    /**
     * 示例5：固定锁键
     */
    @DistributedLockable(key = "order:create")
    public void example5_FixedKey() {
        log.info("创建订单");
        // 方法执行前自动加锁，执行后自动释放
    }

    /**
     * 示例6：SpEL 表达式 - 参数值
     */
    @DistributedLockable(key = "'appt:' + #scheduleId", waitTime = 3, leaseTime = 30)
    public void example6_SpEL_Param(Long scheduleId) {
        log.info("扣减号源: scheduleId={}", scheduleId);
        decreaseSlots(scheduleId);
    }

    /**
     * 示例7：SpEL 表达式 - 对象属性
     */
    @DistributedLockable(key = "'user:' + #dto.userId", waitTime = 5, leaseTime = 60)
    public void example7_SpEL_Object(UserDTO dto) {
        log.info("更新用户: userId={}", dto.getUserId());
        updateUserInfo(dto.getUserId());
    }

    /**
     * 示例8：多参数拼接
     */
    @DistributedLockable(key = "'order:' + #userId + ':' + #productId", waitTime = 3, leaseTime = 30, fairLock = false)
    public void example8_MultiParam(String userId, String productId) {
        log.info("创建订单: userId={}, productId={}", userId, productId);
    }

    /**
     * 示例9：自定义失败策略 - 返回 null
     */
    @DistributedLockable(key = "'payment:' + #orderId", waitTime = 5, leaseTime = 60, failStrategy = DistributedLockable.LockFailStrategy.RETURN_NULL, errorMessage = "支付处理中，请勿重复操作")
    public String example9_ReturnNull(String orderId) {
        log.info("处理支付: orderId={}", orderId);
        return "支付成功";
    }

    /**
     * 示例10：自定义失败策略 - 返回 false
     */
    @DistributedLockable(key = "'refund:' + #orderId", failStrategy = DistributedLockable.LockFailStrategy.RETURN_FALSE)
    public boolean example10_ReturnFalse(String orderId) {
        log.info("处理退款: orderId={}", orderId);
        return true;
    }

    /**
     * 示例11：公平锁（注解方式）
     */
    @DistributedLockable(key = "'queue:' + #userId", fairLock = true, waitTime = 10, leaseTime = 60)
    public void example11_FairLock_Annotation(String userId) {
        log.info("排队处理: userId={}", userId);
        // FIFO 保证先到先得
    }

    // ==================== 模拟业务方法 ====================

    private void decreaseSlots(Long scheduleId) {
        log.info("扣减号源: scheduleId={}", scheduleId);
        // 模拟数据库操作
    }

    private void updateUserInfo(String userId) {
        log.info("更新用户信息: userId={}", userId);
        // 模拟数据库操作
    }

    private String readCache(String key) {
        return "cached-data";
    }

    private void writeCache(String key, String value) {
        log.info("写入缓存: key={}, value={}", key, value);
    }

    /**
     * 模拟 DTO
     */
    public static class UserDTO {
        private String userId;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
