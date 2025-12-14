package me.jianwen.mediask.common.lock.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解（声明式锁）
 * <p>
 * 使用场景：通过 AOP 自动加锁/解锁，简化代码编写
 * 支持 SpEL 表达式动态生成锁键
 * </p>
 * <p>
 * 使用示例：
 * 
 * <pre>
 * // 1. 固定锁键
 * {@literal @}DistributedLockable(key = "order:create")
 * public void createOrder() { ... }
 *
 * // 2. SpEL 表达式（参数值）
 * {@literal @}DistributedLockable(key = "'appt:' + #dto.scheduleId")
 * public void createAppointment(ApptCreateDTO dto) { ... }
 *
 * // 3. SpEL 表达式（对象属性）
 * {@literal @}DistributedLockable(key = "'user:' + #user.id")
 * public void updateUser(User user) { ... }
 *
 * // 4. 自定义超时和租约时间
 * {@literal @}DistributedLockable(
 *     key = "'payment:' + #orderId",
 *     waitTime = 5,
 *     leaseTime = 60
 * )
 * public void processPayment(String orderId) { ... }
 * </pre>
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLockable {

    /**
     * 锁键（支持 SpEL 表达式）
     * <p>
     * 示例：
     * - 固定值："order:create"
     * - 参数值："'appt:' + #dto.scheduleId"
     * - 对象属性："'user:' + #user.id"
     * - 多参数："'order:' + #userId + ':' + #productId"
     * </p>
     */
    String key();

    /**
     * 等待获取锁的最大时间（默认使用配置中的 defaultWaitTime）
     * <p>
     * -1：使用全局配置的默认值
     * >0：自定义等待时间
     * </p>
     */
    long waitTime() default -1;

    /**
     * 锁自动释放时间（默认使用配置中的 defaultLeaseTime）
     * <p>
     * -1：启用看门狗机制（自动续期）
     * >0：固定租约时间
     * </p>
     */
    long leaseTime() default -1;

    /**
     * 时间单位（默认：秒）
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 获取锁失败时的处理策略
     * <p>
     * THROW_EXCEPTION：抛出 LockTimeoutException（默认）
     * RETURN_NULL：返回 null
     * RETURN_FALSE：返回 false（仅限 boolean 返回值）
     * </p>
     */
    LockFailStrategy failStrategy() default LockFailStrategy.THROW_EXCEPTION;

    /**
     * 是否使用公平锁
     * <p>
     * true：先到先得（FIFO）
     * false：非公平锁（默认）
     * </p>
     */
    boolean fairLock() default false;

    /**
     * 锁获取失败时的错误提示信息（可选）
     */
    String errorMessage() default "系统繁忙，请稍后重试";

    /**
     * 锁获取失败策略枚举
     */
    enum LockFailStrategy {
        /**
         * 抛出 LockTimeoutException 异常
         */
        THROW_EXCEPTION,

        /**
         * 返回 null（适用于对象返回值）
         */
        RETURN_NULL,

        /**
         * 返回 false（仅适用于 boolean 返回值）
         */
        RETURN_FALSE
    }
}
