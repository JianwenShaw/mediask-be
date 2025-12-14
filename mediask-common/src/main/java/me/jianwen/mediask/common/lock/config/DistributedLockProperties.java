package me.jianwen.mediask.common.lock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁配置属性
 * <p>
 * 配置前缀：mediask.lock
 * 使用示例（application.yml）：
 * 
 * <pre>
 * mediask:
 *   lock:
 *     enabled: true
 *     key-prefix: "mediask:lock:"
 *     default-wait-time: 3
 *     default-lease-time: 30
 *     use-fair-lock: false
 * </pre>
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
@Data
@ConfigurationProperties(prefix = "mediask.lock")
public class DistributedLockProperties {

    /**
     * 是否启用分布式锁
     * 默认：true
     */
    private boolean enabled = true;

    /**
     * 锁键统一前缀
     * 作用：避免与其他业务的 Redis key 冲突
     * 默认："mediask:lock:"
     */
    private String keyPrefix = "mediask:lock:";

    /**
     * 默认等待时间（秒）
     * 说明：尝试获取锁的最大等待时间
     * 默认：3秒
     */
    private long defaultWaitTime = 3;

    /**
     * 默认租约时间（秒）
     * 说明：锁的自动释放时间，防止死锁
     * -1：启用看门狗机制（自动续期，适合执行时间不确定的场景）
     * >0：固定租约时间（适合执行时间可预估的场景）
     * 默认：30秒
     */
    private long defaultLeaseTime = 30;

    /**
     * 是否使用公平锁
     * true：先到先得（FIFO），性能略低
     * false：非公平锁（默认），性能更高
     * 默认：false
     */
    private boolean useFairLock = false;

    /**
     * 看门狗续期间隔（秒）
     * 说明：当 leaseTime = -1 时，看门狗每隔该时间自动续期
     * 默认：10秒（Redisson 默认为 30 秒的 1/3）
     */
    private long watchdogTimeout = 10;

    /**
     * 是否启用锁性能监控
     * 说明：记录锁获取成功率、平均等待时间等指标
     * 默认：false（避免影响性能）
     */
    private boolean enableMetrics = false;
}
