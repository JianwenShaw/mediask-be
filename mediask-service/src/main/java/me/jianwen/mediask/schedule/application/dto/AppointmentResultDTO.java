package me.jianwen.mediask.schedule.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约结果DTO（创建预约后返回）
 *
 * @author jianwen
 */
@Data
@Builder
public class AppointmentResultDTO {

    /**
     * 预约ID
     */
    private Long appointmentId;

    /**
     * 预约单号
     */
    private String apptNo;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 科室名称
     */
    private String departmentName;

    /**
     * 就诊日期
     */
    private LocalDate apptDate;

    /**
     * 时段描述
     */
    private String timePeriodDesc;

    /**
     * 具体时间
     */
    private LocalTime apptTime;

    /**
     * 预约状态
     */
    private String status;

    /**
     * 挂号费
     */
    private BigDecimal apptFee;

    /**
     * 支付截止时间
     */
    private LocalDateTime payDeadline;
}
