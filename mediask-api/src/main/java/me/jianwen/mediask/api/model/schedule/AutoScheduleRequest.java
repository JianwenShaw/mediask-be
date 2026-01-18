package me.jianwen.mediask.api.model.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 自动排班请求（API层）
 */
@Data
public class AutoScheduleRequest {

    /**
     * 医生ID
     */
    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 策略名称（可选）
     * PERIODIC - 周期性排班
     * CUSTOM_DATE - 自定义日期排班
     */
    private String strategyName;

    /**
     * 生效的星期几（1-7）
     */
    @NotEmpty(message = "工作日不能为空")
    private Set<DayOfWeek> workDays;

    /**
     * 生效的时段代码集合
     */
    @NotEmpty(message = "工作时段不能为空")
    private Set<Integer> timePeriodCodes;

    /**
     * 每个时段的号源数
     */
    @NotNull(message = "号源数不能为空")
    @Min(value = 1, message = "号源数至少为1")
    private Integer slotsPerPeriod;

    /**
     * 每个号源的时长（分钟）
     */
    @NotNull(message = "号源时长不能为空")
    @Min(value = 5, message = "号源时长至少为5分钟")
    private Integer slotDurationMinutes;

    /**
     * 是否排除节假日
     */
    private Boolean excludeHolidays = false;
}
