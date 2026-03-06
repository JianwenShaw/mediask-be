package me.jianwen.mediask.user.domain.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoctorPageQuery {

    private Long userId;
    private String doctorCode;
    private Long hospitalId;
    private Long departmentId;
    private Integer status;
    private String keyword;
    private Integer pageNum;
    private Integer pageSize;
}
