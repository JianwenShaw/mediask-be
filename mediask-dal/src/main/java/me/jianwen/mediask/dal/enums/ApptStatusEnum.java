package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 挂号状态枚举
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum ApptStatusEnum {

    /**
     * 待支付
     */
    UNPAID(1, "待支付"),

    /**
     * 已预约
     */
    CONFIRMED(2, "已预约"),

    /**
     * 已就诊
     */
    VISITED(3, "已就诊"),

    /**
     * 已取消
     */
    CANCELLED(4, "已取消"),

    /**
     * 爽约
     */
    ABSENT(5, "爽约");

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
    public static ApptStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的挂号状态: " + code));
    }
}
