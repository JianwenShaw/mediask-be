package me.jianwen.mediask.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 * <p>
 * 基于 Jackson 封装的 JSON 序列化/反序列化工具
 * </p>
 *
 * @author jianwen
 */
@Slf4j
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // 注册 Java 8 时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        // 序列化时忽略 null 值
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 反序列化时忽略未知属性
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 禁用日期作为时间戳
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private JsonUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 获取 ObjectMapper 实例
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 对象转 JSON 字符串
     *
     * @param object 对象
     * @return JSON 字符串，转换失败返回 null
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象序列化为 JSON 失败", e);
            return null;
        }
    }

    /**
     * 对象转 JSON 字符串（格式化输出）
     *
     * @param object 对象
     * @return 格式化的 JSON 字符串
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象序列化为 JSON 失败", e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 对象实例，转换失败返回 null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化失败, json={}", json, e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象（支持泛型）
     *
     * @param json          JSON 字符串
     * @param typeReference 类型引用
     * @param <T>           类型参数
     * @return 对象实例，转换失败返回 null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化失败, json={}", json, e);
            return null;
        }
    }

    /**
     * JSON 字符串转 List
     *
     * @param json  JSON 字符串
     * @param clazz 元素类型
     * @param <T>   类型参数
     * @return List 对象，转换失败返回空列表
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化为 List 失败, json={}", json, e);
            return Collections.emptyList();
        }
    }

    /**
     * JSON 字符串转 Map
     *
     * @param json JSON 字符串
     * @return Map 对象，转换失败返回空 Map
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON 反序列化为 Map 失败, json={}", json, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 对象转 Map
     *
     * @param object 对象
     * @return Map 对象
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object object) {
        if (object == null) {
            return Collections.emptyMap();
        }
        return OBJECT_MAPPER.convertValue(object, Map.class);
    }

    /**
     * Map 转对象
     *
     * @param map   Map 对象
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 对象实例
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(map, clazz);
    }
}
