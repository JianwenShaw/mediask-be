package me.jianwen.mediask.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 删除标记枚举
 *
 * @author jianwen
 */
@Getter
@AllArgsConstructor
public enum DeletedEnum {

    /**
     * 未删除
     */
    NOT_DELETED(0, "未删除"),

    /**
     * 已删除
     */
    DELETED(1, "已删除");

    /**
     * 标记值
     */
    private final Integer code;

    /**
     * 描述
     */
    private final String desc;

    /**
     * 判断是否已删除
     *
     * @param code 标记值
     * @return true 表示已删除
     */
    public static boolean isDeleted(Integer code) {
        return DELETED.getCode().equals(code);
    }
}
