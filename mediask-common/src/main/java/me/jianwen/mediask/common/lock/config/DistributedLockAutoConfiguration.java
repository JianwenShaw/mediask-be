package me.jianwen.mediask.common.lock.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.lock.DistributedLockFactory;
import me.jianwen.mediask.common.lock.impl.RedissonLockFactory;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 分布式锁自动配置类
 * <p>
 * 职责：自动装配分布式锁相关 Bean
 * 生效条件：
 * 1. classpath 存在 RedissonClient（引入 redisson-spring-boot-starter）
 * 2. 配置 mediask.lock.enabled=true（默认开启）
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
@Configuration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "mediask.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DistributedLockProperties.class)
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAutoConfiguration {

    private final DistributedLockProperties properties;

    /**
     * 注册分布式锁工厂
     * <p>
     * ConditionalOnMissingBean：允许用户自定义实现覆盖默认实现
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public DistributedLockFactory distributedLockFactory(RedissonClient redissonClient) {
        log.info("初始化分布式锁工厂: keyPrefix={}, defaultWaitTime={}s, defaultLeaseTime={}s, useFairLock={}",
                properties.getKeyPrefix(),
                properties.getDefaultWaitTime(),
                properties.getDefaultLeaseTime(),
                properties.isUseFairLock());

        return new RedissonLockFactory(redissonClient, properties);
    }

    /**
     * 配置校验（可选）
     */
    @Bean
    public DistributedLockConfigValidator distributedLockConfigValidator() {
        return new DistributedLockConfigValidator(properties);
    }

    /**
     * 配置校验器（内部类）
     */
    @RequiredArgsConstructor
    static class DistributedLockConfigValidator {
        private final DistributedLockProperties properties;

        /**
         * 启动时校验配置合法性
         */
        public void validate() {
            if (properties.getDefaultWaitTime() <= 0) {
                log.warn("defaultWaitTime 配置异常，已重置为 3 秒");
                properties.setDefaultWaitTime(3);
            }

            if (properties.getDefaultLeaseTime() < -1) {
                log.warn("defaultLeaseTime 配置异常，已重置为 30 秒");
                properties.setDefaultLeaseTime(30);
            }

            if (properties.getKeyPrefix() == null || properties.getKeyPrefix().trim().isEmpty()) {
                log.warn("keyPrefix 配置为空，已重置为默认值");
                properties.setKeyPrefix("mediask:lock:");
            }

            log.debug("分布式锁配置校验通过");
        }
    }
}
