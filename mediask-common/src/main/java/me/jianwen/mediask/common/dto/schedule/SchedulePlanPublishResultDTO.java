package me.jianwen.mediask.common.dto.schedule;

import java.util.List;

public record SchedulePlanPublishResultDTO(
        Long planId,
        String planCode,
        Integer versionNo,
        String action,
        Integer createdSchedules,
        Integer closedSchedules,
        List<String> warnings
) {
}
