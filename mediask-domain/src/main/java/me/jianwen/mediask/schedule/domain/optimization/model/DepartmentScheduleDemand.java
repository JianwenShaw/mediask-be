package me.jianwen.mediask.schedule.domain.optimization.model;

import java.time.LocalDate;

public record DepartmentScheduleDemand(
        Long departmentId,
        LocalDate date,
        Integer periodCode,
        Integer requiredDoctors,
        Integer minSeniorDoctors
) {
}
