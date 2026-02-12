package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ScheduleTemplateRuleCommand {

    private Integer weekday;
    private Integer timePeriodCode;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;
    private Integer slotDurationMinutes;
    private Integer slotCapacity;
    private BigDecimal fee;
}
