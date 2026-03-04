package me.jianwen.mediask.infra.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.jianwen.mediask.common.util.JsonUtil;
import me.jianwen.mediask.domain.cache.NullValue;
import org.springframework.stereotype.Component;

/**
 * 缓存序列化服务。
 *
 * <p>负责缓存值在 Java 对象与 JSON 字符串之间的转换，统一处理：
 * <ul>
 *   <li>空值占位符 {@link NullValue} 的序列化/反序列化</li>
 *   <li>复杂泛型类型（如 {@code List<UserDTO>}）的反序列化</li>
 *   <li>序列化异常的统一包装</li>
 * </ul>
 *
 * <p>底层复用项目全局的 {@link JsonUtil#getObjectMapper()} 实例，
 * 确保日期格式、命名策略等与业务代码一致。
 */
@Component
public class CacheSerialization {

    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    /**
     * 序列化对象为 JSON 字符串。
     *
     * <p>{@link NullValue} 实例会被序列化为固定标记 {@code "__NULL__"}，
     * 与真实业务数据区分。
     *
     * @param value 待序列化的对象
     * @return JSON 字符串
     * @throws CacheSerializationException 序列化失败时抛出
     */
    public String serialize(Object value) {
        if (value instanceof NullValue) {
            return NullValue.SERIALIZED_MARKER;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new CacheSerializationException("缓存序列化失败: " + value.getClass().getSimpleName(), exception);
        }
    }

    /**
     * 反序列化 JSON 字符串为指定类型。
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @param <T>  值类型
     * @return 反序列化后的对象；如果 JSON 为空值标记则返回 null
     * @throws CacheSerializationException 反序列化失败时抛出
     */
    public <T> T deserialize(String json, Class<T> type) {
        if (json == null) {
            return null;
        }
        if (NullValue.SERIALIZED_MARKER.equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new CacheSerializationException("缓存反序列化失败: type=" + type.getSimpleName(), exception);
        }
    }

    /**
     * 反序列化 JSON 字符串为泛型类型。
     *
     * <p>适用于 {@code List<UserDTO>}、{@code Map<String, Object>} 等复杂泛型。
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           值类型
     * @return 反序列化后的对象
     * @throws CacheSerializationException 反序列化失败时抛出
     */
    public <T> T deserialize(String json, TypeReference<T> typeReference) {
        if (json == null) {
            return null;
        }
        if (NullValue.SERIALIZED_MARKER.equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new CacheSerializationException("缓存反序列化失败: typeRef=" + typeReference.getType(), exception);
        }
    }

    /**
     * 判断 JSON 字符串是否为空值标记
     */
    public boolean isNullMarker(String json) {
        return NullValue.SERIALIZED_MARKER.equals(json);
    }

    /**
     * 缓存序列化异常（运行时异常，不强制调用方 catch）
     */
    public static class CacheSerializationException extends RuntimeException {
        public CacheSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
