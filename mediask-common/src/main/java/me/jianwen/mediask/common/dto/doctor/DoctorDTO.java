package me.jianwen.mediask.common.dto.doctor;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DoctorDTO {

    private Long doctorId;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private Long hospitalId;
    private String hospitalName;
    private Long departmentId;
    private String departmentName;
    private String doctorCode;
    private String title;
    private String specialty;
    private String introduction;
    private BigDecimal consultationFee;
    private String licenseNumber;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
