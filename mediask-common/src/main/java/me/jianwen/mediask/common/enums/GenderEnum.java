package me.jianwen.mediask.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 性别枚举
 *
 * @author jianwen
 */
@Getter
@AllArgsConstructor
public enum GenderEnum {

    /**
     * 未知
     */
    UNKNOWN(0, "未知"),

    /**
     * 男
     */
    MALE(1, "男"),

    /**
     * 女
     */
    FEMALE(2, "女");

    /**
     * 性别码
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 根据性别码获取枚举
     *
     * @param code 性别码
     * @return GenderEnum
     */
    public static GenderEnum fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
