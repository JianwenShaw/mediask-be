package me.jianwen.mediask.schedule.domain.rule;

import java.time.LocalDate;

public record DoctorTimeOff(
        Long doctorId,
        LocalDate startDate,
        LocalDate endDate,
        Integer periodCode
) {
}
