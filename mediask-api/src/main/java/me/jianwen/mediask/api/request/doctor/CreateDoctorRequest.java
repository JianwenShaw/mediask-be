package me.jianwen.mediask.api.request.doctor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDoctorRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "医院ID不能为空")
    private Long hospitalId;

    @NotNull(message = "科室ID不能为空")
    private Long departmentId;

    @NotBlank(message = "医生编码不能为空")
    private String doctorCode;

    private String title;
    private String specialty;
    private String introduction;
    private BigDecimal consultationFee;
    private String licenseNumber;
    private Integer status;
}
