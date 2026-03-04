package me.jianwen.mediask.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 配置类。
 *
 * <p>使用 Spring Data Redis（Lettuce）作为缓存客户端，分布式锁使用 Redisson。
 *
 * <h3>提供的 Bean</h3>
 * <ul>
 *   <li>{@link StringRedisTemplate} — 所有缓存操作统一使用 String 序列化（值由 CacheSerialization 处理）</li>
 *   <li>{@link RedisMessageListenerContainer} — 用于缓存失效广播（Redis Pub/Sub）</li>
 * </ul>
 *
 * @author jianwen
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate，统一用于所有缓存和简单字符串操作。
     *
     * <p>不再提供 {@code RedisTemplate<String, Object>}：
     * 旧的 JSON 序列化模板使用了 {@code DefaultTyping.NON_FINAL}，存在反序列化安全风险，
     * 且项目中从未实际使用。所有缓存值的序列化/反序列化由 CacheSerialization 统一处理。
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Redis Pub/Sub 消息监听容器。
     *
     * <p>供 {@link me.jianwen.mediask.infra.cache.CacheInvalidationBus} 注册
     * 缓存失效消息的监听器，实现多实例间本地缓存（L1）的自动失效。
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}

