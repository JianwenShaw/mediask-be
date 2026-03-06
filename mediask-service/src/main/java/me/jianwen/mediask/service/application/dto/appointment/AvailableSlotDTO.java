package me.jianwen.mediask.service.application.dto.appointment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

/**
 * 可预约时段DTO（Service层与API层共享）
 */
@Data
@Builder
public class AvailableSlotDTO {

    private Long slotId;

    private Long scheduleId;

    private LocalTime time;

    private Boolean available;
}
