package me.jianwen.mediask.api.request.schedule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class ScheduleTemplateRuleRequest {

    @NotNull(message = "星期不能为空")
    @Min(value = 1, message = "星期范围为1-7")
    @Max(value = 7, message = "星期范围为1-7")
    private Integer weekday;

    @NotNull(message = "时段不能为空")
    @Min(value = 1, message = "时段范围为1-3")
    @Max(value = 3, message = "时段范围为1-3")
    private Integer timePeriodCode;

    @NotNull(message = "开始时间不能为空")
    private LocalTime periodStartTime;

    @NotNull(message = "结束时间不能为空")
    private LocalTime periodEndTime;

    @NotNull(message = "号源时长不能为空")
    @Min(value = 5, message = "号源时长至少为5分钟")
    private Integer slotDurationMinutes;

    @NotNull(message = "号源容量不能为空")
    @Min(value = 1, message = "号源容量至少为1")
    private Integer slotCapacity;

    private BigDecimal fee;
}
