package me.jianwen.mediask.api.model.appointment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约结果响应（API层）
 */
@Data
@Builder
public class AppointmentResultResponse {

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
