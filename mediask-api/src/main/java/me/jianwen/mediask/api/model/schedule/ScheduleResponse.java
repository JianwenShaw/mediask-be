package me.jianwen.mediask.api.model.schedule;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排班响应（API层）
 */
@Data
@Builder
public class ScheduleResponse {

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
