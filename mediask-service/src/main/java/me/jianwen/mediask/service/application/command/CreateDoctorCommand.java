package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDoctorCommand {

    private Long userId;
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
