package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 用户类型枚举
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {

    /**
     * 患者
     */
    PATIENT(1, "患者"),

    /**
     * 医生
     */
    DOCTOR(2, "医生"),

    /**
     * 管理员
     */
    ADMIN(3, "管理员");

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
    public static UserTypeEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的用户类型: " + code));
    }
}
