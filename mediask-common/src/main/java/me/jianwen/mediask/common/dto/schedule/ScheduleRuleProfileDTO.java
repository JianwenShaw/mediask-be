package me.jianwen.mediask.common.dto.schedule;

/**
 * 排班规则配置详情。
 */
public record ScheduleRuleProfileDTO(
        Long profileId,
        Long departmentId,
        String profileCode,
        String profileName,
        Integer versionNo,
        String profileStatus,
        String constraintDslJson,
        String description
) {
}
