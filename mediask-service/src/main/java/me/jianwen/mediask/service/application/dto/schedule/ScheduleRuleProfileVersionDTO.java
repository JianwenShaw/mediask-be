package me.jianwen.mediask.service.application.dto.schedule;

import java.time.LocalDateTime;

/**
 * 排班规则配置版本信息。
 */
public record ScheduleRuleProfileVersionDTO(
        Long profileId,
        Long departmentId,
        String profileCode,
        String profileName,
        Integer versionNo,
        String profileStatus,
        LocalDateTime updatedAt
) {
}
