package me.jianwen.mediask.common.dto.schedule;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班信息DTO（Service层与API层共享）
 */
@Data
@Builder
public class ScheduleDTO {

    private Long scheduleId;
    private Long doctorId;
    private LocalDate scheduleDate;
    private Integer timePeriodCode;
    private String timePeriodDesc;
    private Integer totalSlots;
    private Integer availableSlots;
    private Integer statusCode;
    private String statusDesc;
    private Integer slotDurationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
