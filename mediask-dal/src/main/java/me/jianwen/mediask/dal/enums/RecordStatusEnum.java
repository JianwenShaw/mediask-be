package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 病历状态枚举
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum RecordStatusEnum {

    /**
     * 草稿
     */
    DRAFT(1, "草稿"),

    /**
     * 已提交
     */
    SUBMITTED(2, "已提交"),

    /**
     * 已归档
     */
    ARCHIVED(3, "已归档");

    /**
     * 数据库存储值
     */
    @EnumValue
    private final Integer code;

    /**
     * 描述
     */
    @JsonValue
    private final String desc;

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 枚举
     */
    public static RecordStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的病历状态: " + code));
    }
}
