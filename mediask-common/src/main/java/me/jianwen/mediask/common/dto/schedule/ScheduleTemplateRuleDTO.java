package me.jianwen.mediask.common.dto.schedule;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleTemplateRuleDTO {

    private Long id;
    private Integer weekday;
    private Integer timePeriodCode;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;
    private Integer slotDurationMinutes;
    private Integer slotCapacity;
    private BigDecimal fee;
}
