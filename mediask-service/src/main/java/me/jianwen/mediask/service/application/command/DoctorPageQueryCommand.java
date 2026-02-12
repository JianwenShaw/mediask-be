package me.jianwen.mediask.service.application.command;

import lombok.Data;

@Data
public class DoctorPageQueryCommand {

    private Long userId;
    private String doctorCode;
    private Long hospitalId;
    private Long departmentId;
    private Integer status;
    private String keyword;
    private Integer pageNum;
    private Integer pageSize;
}
