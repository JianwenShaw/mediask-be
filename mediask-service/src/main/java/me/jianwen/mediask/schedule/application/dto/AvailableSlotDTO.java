package me.jianwen.mediask.schedule.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

/**
 * 可预约时段DTO
 *
 * @author jianwen
 */
@Data
@Builder
public class AvailableSlotDTO {

    /**
     * 号源时段ID
     */
    private Long slotId;

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 具体时间
     */
    private LocalTime time;

    /**
     * 是否可预约
     */
    private Boolean available;
}
