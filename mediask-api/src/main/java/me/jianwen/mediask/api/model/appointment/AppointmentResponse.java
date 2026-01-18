package me.jianwen.mediask.api.model.appointment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约详情响应（API层）
 */
@Data
@Builder
public class AppointmentResponse {

    private Long id;
    private String apptNo;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private String departmentName;
    private Long hospitalId;
    private String hospitalName;
    private Long scheduleId;
    private LocalDate apptDate;
    private String timePeriodDesc;
    private LocalTime apptTime;
    private Integer statusCode;
    private String statusDesc;
    private String chiefComplaint;
    private BigDecimal apptFee;
    private LocalDateTime paidAt;
    private LocalDateTime visitedAt;
    private LocalDateTime createdAt;
}
