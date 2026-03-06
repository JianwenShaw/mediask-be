package me.jianwen.mediask.api.request.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 创建预约请求（API层）
 */
@Data
public class CreateAppointmentRequest {

    /**
     * 排班ID
     */
    @NotNull(message = "排班ID不能为空")
    private Long scheduleId;

    /**
     * 就诊日期
     */
    @NotNull(message = "就诊日期不能为空")
    private LocalDate apptDate;

    /**
     * 时段代码：1-上午 2-下午 3-晚上
     */
    @NotNull(message = "时段不能为空")
    private Integer timePeriodCode;

    /**
     * 具体就诊时间
     */
    @NotNull(message = "就诊时间不能为空")
    private LocalTime apptTime;

    /**
     * 主诉/症状描述
     */
    private String chiefComplaint;
}
