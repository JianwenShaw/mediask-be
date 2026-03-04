package me.jianwen.mediask.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.util.JsonUtil;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 缓存失效广播总线（基于 Redis Pub/Sub）。
 *
 * <p>解决多实例部署下本地缓存（L1 / Caffeine）一致性问题：
 * 当一个实例执行 {@link #publish} 时，其他所有实例会收到消息并自动清除对应的本地缓存。
 *
 * <h3>工作流程</h3>
 * <pre>
 *   实例 A 调用 evict()
 *       ↓
 *   1. 清除本实例 L1
 *   2. 清除 L2 (Redis)
 *   3. 发布失效消息到 Redis Pub/Sub Channel
 *       ↓
 *   实例 B/C/D 收到消息
 *       ↓
 *   4. 忽略自身发出的消息（通过 instanceId 判断）
 *   5. 清除本地 L1 对应的缓存条目
 * </pre>
 *
 * <h3>消息丢失兜底</h3>
 * <p>Redis Pub/Sub 是 at-most-once 投递（实例离线期间的消息不会补发）。
 * 兜底策略：L1 缓存设有 TTL（{@link me.jianwen.mediask.domain.cache.CacheDefinition#localTtl()}），
 * 即使 Pub/Sub 消息丢失，TTL 过期后也会自然淘汰过期数据，保证最终一致性。
 */
@Slf4j
@Component
public class CacheInvalidationBus {

    /**
     * Redis Pub/Sub 频道名称
     */
    private static final String CHANNEL = "mediask:cache:invalidation";

    /**
     * 当前实例唯一标识，用于过滤自身发出的消息
     */
    private final String instanceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    private final StringRedisTemplate redisTemplate;
    private final CaffeineCacheManager caffeineCacheManager;
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    public CacheInvalidationBus(
            StringRedisTemplate redisTemplate,
            CaffeineCacheManager caffeineCacheManager,
            RedisMessageListenerContainer listenerContainer) {
        this.redisTemplate = redisTemplate;
        this.caffeineCacheManager = caffeineCacheManager;

        // 注册消息监听器
        MessageListenerAdapter adapter = new MessageListenerAdapter(new InvalidationListener(), "onMessage");
        adapter.afterPropertiesSet();
        listenerContainer.addMessageListener(adapter, new ChannelTopic(CHANNEL));

        log.info("缓存失效广播总线已启动, instanceId={}, channel={}", instanceId, CHANNEL);
    }

    /**
     * 发布缓存失效消息。
     *
     * <p>其他实例收到消息后会自动清除对应的本地缓存。
     * 当前实例的本地缓存应在调用此方法前已经清除。
     *
     * @param cacheName 缓存名称（{@link me.jianwen.mediask.domain.cache.CacheDefinition#name()}）
     * @param key       完整缓存键（由 {@link CacheKeyGenerator} 生成），传 null 表示清除整个缓存
     */
    public void publish(String cacheName, String key) {
        try {
            InvalidationMessage message = new InvalidationMessage(instanceId, cacheName, key);
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("发布缓存失效消息: cacheName={}, key={}", cacheName, key);
        } catch (JsonProcessingException exception) {
            log.error("缓存失效消息序列化失败: cacheName={}, key={}", cacheName, key, exception);
        }
    }

    /**
     * 失效消息监听器
     */
    private class InvalidationListener implements MessageListener {

        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String json = new String(message.getBody());
                InvalidationMessage msg = objectMapper.readValue(json, InvalidationMessage.class);

                // 忽略自身发出的消息
                if (instanceId.equals(msg.sourceInstanceId())) {
                    return;
                }

                if (msg.key() == null || msg.key().isEmpty()) {
                    // 清除整个缓存
                    caffeineCacheManager.evictAllByName(msg.cacheName());
                    log.debug("收到跨实例缓存失效广播（全量清除）: from={}, cacheName={}",
                            msg.sourceInstanceId(), msg.cacheName());
                } else {
                    // 清除单个键
                    caffeineCacheManager.evictByName(msg.cacheName(), msg.key());
                    log.debug("收到跨实例缓存失效广播: from={}, cacheName={}, key={}",
                            msg.sourceInstanceId(), msg.cacheName(), msg.key());
                }
            } catch (Exception exception) {
                log.error("处理缓存失效消息异常", exception);
            }
        }
    }

    /**
     * 缓存失效消息结构
     *
     * @param sourceInstanceId 发送方实例标识
     * @param cacheName        缓存名称
     * @param key              缓存键（null 表示清除整个缓存）
     */
    public record InvalidationMessage(
            String sourceInstanceId,
            String cacheName,
            String key
    ) {
    }
}
