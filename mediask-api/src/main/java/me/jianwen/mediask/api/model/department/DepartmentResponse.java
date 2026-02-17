package me.jianwen.mediask.api.model.department;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DepartmentResponse {
    private Long departmentId;
    private Long hospitalId;
    private String deptCode;
    private String deptName;
    private LocalDateTime createdAt;
}
