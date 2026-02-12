package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 排班状态枚举
 */
@Getter
@AllArgsConstructor
public enum ScheduleStatusEnum {

    CLOSED(0, "停诊"),
    OPEN(1, "开放"),
    FULL(2, "约满"),
    EXPIRED(3, "已过期");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String desc;

    public static ScheduleStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的排班状态: " + code));
    }
}
