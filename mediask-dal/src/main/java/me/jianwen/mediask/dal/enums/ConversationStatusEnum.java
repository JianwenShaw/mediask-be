package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI会话状态枚举
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum ConversationStatusEnum {

    /**
     * 进行中
     */
    ONGOING(1, "进行中"),

    /**
     * 已结束
     */
    ENDED(2, "已结束");

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
    public static ConversationStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的会话状态: " + code));
    }
}
