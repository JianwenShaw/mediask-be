package me.jianwen.mediask.api.model.appointment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

/**
 * 可预约时段响应（API层）
 */
@Data
@Builder
public class AvailableSlotResponse {

    private Long slotId;
    private Long scheduleId;
    private LocalTime time;
    private Boolean available;
}
