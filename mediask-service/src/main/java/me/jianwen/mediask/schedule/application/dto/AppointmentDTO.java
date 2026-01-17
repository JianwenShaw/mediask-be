package me.jianwen.mediask.schedule.application.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约详情DTO
 *
 * @author jianwen
 */
@Data
@Builder
public class AppointmentDTO {

    /**
     * 预约ID
     */
    private Long id;

    /**
     * 预约单号
     */
    private String apptNo;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 科室ID
     */
    private Long departmentId;

    /**
     * 科室名称
     */
    private String departmentName;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 排班ID
     */
    private Long scheduleId;

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
     * 预约状态码
     */
    private Integer statusCode;

    /**
     * 预约状态描述
     */
    private String statusDesc;

    /**
     * 主诉
     */
    private String chiefComplaint;

    /**
     * 挂号费
     */
    private BigDecimal apptFee;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 就诊时间
     */
    private LocalDateTime visitedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
