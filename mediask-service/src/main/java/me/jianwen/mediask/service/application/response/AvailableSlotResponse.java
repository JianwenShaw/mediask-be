package me.jianwen.mediask.service.application.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

/**
 * 可预约时段响应（应用层）
 */
@Data
@Builder
public class AvailableSlotResponse {

    private Long slotId;

    private Long scheduleId;

    private LocalTime time;

    private Boolean available;
}
