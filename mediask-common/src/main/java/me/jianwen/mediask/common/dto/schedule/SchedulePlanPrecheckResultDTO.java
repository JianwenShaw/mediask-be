package me.jianwen.mediask.common.dto.schedule;

import java.util.List;

public record SchedulePlanPrecheckResultDTO(
        Long planId,
        String planCode,
        Integer versionNo,
        String mode,
        boolean wouldBlock,
        Integer conflictCount,
        Integer toCreateSchedules,
        Integer toCloseSchedules,
        List<String> conflicts
) {
}
