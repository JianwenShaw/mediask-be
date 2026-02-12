package me.jianwen.mediask.schedule.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ScheduleTemplateRule {

    private Long id;
    private Long templateId;
    private Integer weekday;
    private Integer timePeriodCode;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;
    private Integer slotDurationMinutes;
    private Integer slotCapacity;
    private BigDecimal fee;
    private Integer status;
}
