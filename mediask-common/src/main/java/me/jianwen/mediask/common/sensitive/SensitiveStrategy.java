package me.jianwen.mediask.common.sensitive;

import java.util.function.Function;

/**
 * 敏感数据脱敏策略
 *
 * @author jianwen
 */
public enum SensitiveStrategy {

    /**
     * 手机号脱敏
     * 示例：138****1234
     */
    PHONE(value -> {
        if (value == null || value.length() != 11) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(7);
    }),

    /**
     * 身份证号脱敏
     * 示例：110***********1234
     */
    ID_CARD(value -> {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 3) + "***********" + value.substring(value.length() - 4);
    }),

    /**
     * 姓名脱敏
     * 示例：张* 或 张*明
     */
    NAME(value -> {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 2) {
            return value.charAt(0) + "*";
        }
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }),

    /**
     * 邮箱脱敏
     * 示例：t***@example.com
     */
    EMAIL(value -> {
        if (value == null || !value.contains("@")) {
            return value;
        }
        int atIndex = value.indexOf("@");
        if (atIndex <= 1) {
            return value;
        }
        return value.charAt(0) + "***" + value.substring(atIndex);
    }),

    /**
     * 地址脱敏
     * 示例：北京市朝阳区***
     */
    ADDRESS(value -> {
        if (value == null || value.length() <= 6) {
            return value;
        }
        return value.substring(0, 6) + "***";
    }),

    /**
     * 银行卡号脱敏
     * 示例：6222****1234
     */
    BANK_CARD(value -> {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }),

    /**
     * 密码脱敏（全部隐藏）
     * 示例：******
     */
    PASSWORD(value -> "******"),

    /**
     * 不脱敏
     */
    NONE(value -> value);

    private final Function<String, String> desensitizer;

    SensitiveStrategy(Function<String, String> desensitizer) {
        this.desensitizer = desensitizer;
    }

    /**
     * 执行脱敏
     *
     * @param value 原始值
     * @return 脱敏后的值
     */
    public String desensitize(String value) {
        return desensitizer.apply(value);
    }
}
