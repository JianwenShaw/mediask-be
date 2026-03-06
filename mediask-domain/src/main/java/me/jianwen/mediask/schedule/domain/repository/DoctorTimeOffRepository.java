package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.rule.DoctorTimeOff;

import java.time.LocalDate;
import java.util.List;

public interface DoctorTimeOffRepository {

    List<DoctorTimeOff> listByDoctorIdsAndDateRange(List<Long> doctorIds, LocalDate startDate, LocalDate endDate);
}
