package me.jianwen.mediask.common.util;

import java.util.UUID;

/**
 * ID 生成工具类
 *
 * @author jianwen
 */
public final class IdUtil {

    private IdUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 生成 UUID（不含横杠）
     *
     * @return 32 位 UUID 字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成 UUID（含横杠）
     *
     * @return 36 位 UUID 字符串
     */
    public static String uuidWithHyphen() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成简短 UUID（前 8 位）
     *
     * @return 8 位 UUID 字符串
     */
    public static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
