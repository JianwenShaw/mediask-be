package me.jianwen.mediask.schedule.domain.rule;

import java.time.LocalDateTime;

/**
 * 排班规则配置版本。
 */
public record ScheduleRuleProfile(
        Long profileId,
        Long departmentId,
        String profileCode,
        String profileName,
        Integer versionNo,
        String profileStatus,
        String constraintDslJson,
        String description,
        Long updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
