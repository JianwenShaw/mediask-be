package me.jianwen.mediask.schedule.application.command;

import lombok.Data;
import me.jianwen.mediask.schedule.domain.valueobject.TimePeriod;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 创建排班命令
 *
 * @author jianwen
 */
@Data
public class CreateScheduleCommand {

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
     * 时段
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

    /**
     * 获取时段枚举
     */
    public TimePeriod getTimePeriod() {
        return TimePeriod.fromCode(timePeriodCode);
    }
}
