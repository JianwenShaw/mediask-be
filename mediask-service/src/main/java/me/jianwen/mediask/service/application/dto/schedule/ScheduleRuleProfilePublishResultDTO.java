package me.jianwen.mediask.service.application.dto.schedule;

/**
 * 规则配置发布/回滚结果。
 */
public record ScheduleRuleProfilePublishResultDTO(
        Long profileId,
        String profileCode,
        Integer versionNo,
        String profileStatus,
        long dslVersion,
        String message
) {
}
