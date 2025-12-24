package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 文档解析状态枚举
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum ParseStatusEnum {

    /**
     * 待解析
     */
    PENDING(0, "待解析"),

    /**
     * 解析中
     */
    PARSING(1, "解析中"),

    /**
     * 解析完成
     */
    COMPLETED(2, "解析完成"),

    /**
     * 解析失败
     */
    FAILED(3, "解析失败");

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
    public static ParseStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的解析状态: " + code));
    }
}
