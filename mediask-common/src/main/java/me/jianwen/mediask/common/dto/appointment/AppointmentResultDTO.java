package me.jianwen.mediask.common.dto.appointment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约结果DTO（Service层与API层共享）
 */
@Data
@Builder
public class AppointmentResultDTO {

    private Long appointmentId;

    private String apptNo;

    private Long doctorId;

    private String doctorName;

    private String departmentName;

    private LocalDate apptDate;

    private String timePeriodDesc;

    private LocalTime apptTime;

    private String status;

    private BigDecimal apptFee;

    private LocalDateTime payDeadline;
}
