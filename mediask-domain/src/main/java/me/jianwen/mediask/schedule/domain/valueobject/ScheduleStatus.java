package me.jianwen.mediask.schedule.domain.valueobject;

import lombok.Getter;

import java.util.Arrays;

/**
 * 排班状态值对象
 *
 * @author jianwen
 */
@Getter
public enum ScheduleStatus {

    /**
     * 开放：可以预约
     */
    OPEN(1, "开放"),

    /**
     * 停诊：医生请假、调休等
     */
    CLOSED(0, "停诊"),

    /**
     * 约满：号源已满
     */
    FULL(2, "约满"),

    /**
     * 已过期：排班日期已过
     */
    EXPIRED(3, "已过期");

    private final Integer code;
    private final String description;

    ScheduleStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 从代码创建
     */
    public static ScheduleStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid schedule status code: " + code));
    }

    /**
     * 是否可以预约
     */
    public boolean canAppointment() {
        return this == OPEN;
    }

    /**
     * 是否可以取消
     */
    public boolean canCancel() {
        return this == OPEN || this == FULL;
    }
}
