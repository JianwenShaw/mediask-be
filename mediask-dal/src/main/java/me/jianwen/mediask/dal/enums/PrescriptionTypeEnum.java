package me.jianwen.mediask.dal.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 处方类型枚举
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum PrescriptionTypeEnum {

    /**
     * 西药处方
     */
    WESTERN_MEDICINE(1, "西药处方"),

    /**
     * 中药处方
     */
    CHINESE_MEDICINE(2, "中药处方"),

    /**
     * 检查处方
     */
    EXAMINATION(3, "检查处方");

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
    public static PrescriptionTypeEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("无效的处方类型: " + code));
    }
}
