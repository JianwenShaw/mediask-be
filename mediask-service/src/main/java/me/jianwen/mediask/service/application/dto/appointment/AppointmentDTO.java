package me.jianwen.mediask.service.application.dto.appointment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约详情DTO（Service层与API层共享）
 */
@Data
@Builder
public class AppointmentDTO {

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
