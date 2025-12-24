package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 时间段枚举
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum TimePeriodEnum {

    /**
     * 上午 (08:00-12:00)
     */
    MORNING(1, "上午"),

    /**
     * 下午 (14:00-18:00)
     */
    AFTERNOON(2, "下午"),

    /**
     * 晚上 (19:00-21:00)
     */
    EVENING(3, "晚上");

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
    public static TimePeriodEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的时间段: " + code));
    }
}
