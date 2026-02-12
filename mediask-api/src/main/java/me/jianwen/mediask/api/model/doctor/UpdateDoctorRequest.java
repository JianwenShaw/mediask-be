package me.jianwen.mediask.api.model.doctor;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDoctorRequest {

    private Long hospitalId;
    private Long departmentId;
    private String doctorCode;
    private String title;
    private String specialty;
    private String introduction;
    private BigDecimal consultationFee;
    private String licenseNumber;
    private Integer status;
}
