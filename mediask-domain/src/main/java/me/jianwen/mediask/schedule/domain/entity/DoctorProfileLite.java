package me.jianwen.mediask.schedule.domain.entity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoctorProfileLite {

    Long doctorId;
    Long userId;
    Long departmentId;
}
