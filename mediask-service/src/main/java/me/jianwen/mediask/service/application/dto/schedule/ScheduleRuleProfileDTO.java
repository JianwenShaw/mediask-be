package me.jianwen.mediask.service.application.dto.schedule;

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
