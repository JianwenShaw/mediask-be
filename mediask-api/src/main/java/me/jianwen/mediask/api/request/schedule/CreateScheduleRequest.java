package me.jianwen.mediask.api.request.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建排班请求（API层）
 */
@Data
public class CreateScheduleRequest {

    /**
     * 医生ID
     */
    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    /**
     * 排班日期
     */
    @NotNull(message = "排班日期不能为空")
    private LocalDate scheduleDate;

    /**
     * 时段代码：1-上午 2-下午 3-晚上
     */
    @NotNull(message = "时段不能为空")
    private Integer timePeriodCode;

    /**
     * 总号源数
     */
    @NotNull(message = "总号源数不能为空")
    @Min(value = 1, message = "总号源数至少为1")
    private Integer totalSlots;

    /**
     * 每个号源的时长（分钟）
     */
    @NotNull(message = "号源时长不能为空")
    @Min(value = 5, message = "号源时长至少为5分钟")
    private Integer slotDurationMinutes;
}
