package me.jianwen.mediask.common.util;

import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 断言工具类
 * <p>
 * 用于参数校验，校验失败时抛出 BizException
 * </p>
 *
 * @author jianwen
 */
public final class AssertUtil {

    private AssertUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 断言对象不为 null
     *
     * @param object    对象
     * @param errorCode 错误码
     */
    public static void notNull(Object object, ErrorCode errorCode) {
        if (object == null) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 断言对象不为 null
     *
     * @param object  对象
     * @param message 错误消息
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言字符串不为空
     *
     * @param str       字符串
     * @param errorCode 错误码
     */
    public static void notBlank(String str, ErrorCode errorCode) {
        if (str == null || str.isBlank()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 断言字符串不为空
     *
     * @param str     字符串
     * @param message 错误消息
     */
    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言集合不为空
     *
     * @param collection 集合
     * @param errorCode  错误码
     */
    public static void notEmpty(Collection<?> collection, ErrorCode errorCode) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 断言集合不为空
     *
     * @param collection 集合
     * @param message    错误消息
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言 Map 不为空
     *
     * @param map       Map
     * @param errorCode 错误码
     */
    public static void notEmpty(Map<?, ?> map, ErrorCode errorCode) {
        if (map == null || map.isEmpty()) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 断言条件为真
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 断言条件为真
     *
     * @param condition 条件
     * @param message   错误消息
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言条件为假
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void isFalse(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 断言条件为假
     *
     * @param condition 条件
     * @param message   错误消息
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言两个对象相等
     *
     * @param obj1      对象1
     * @param obj2      对象2
     * @param errorCode 错误码
     */
    public static void equals(Object obj1, Object obj2, ErrorCode errorCode) {
        if (!Objects.equals(obj1, obj2)) {
            throw new BizException(errorCode);
        }
    }

    /**
     * 断言两个对象相等
     *
     * @param obj1    对象1
     * @param obj2    对象2
     * @param message 错误消息
     */
    public static void equals(Object obj1, Object obj2, String message) {
        if (!Objects.equals(obj1, obj2)) {
            throw new BizException(ErrorCode.PARAM_ERROR, message);
        }
    }

    /**
     * 断言数据存在
     *
     * @param object 对象
     */
    public static void found(Object object) {
        if (object == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
    }

    /**
     * 断言数据存在
     *
     * @param object    对象
     * @param errorCode 错误码
     */
    public static void found(Object object, ErrorCode errorCode) {
        if (object == null) {
            throw new BizException(errorCode);
        }
    }
}
