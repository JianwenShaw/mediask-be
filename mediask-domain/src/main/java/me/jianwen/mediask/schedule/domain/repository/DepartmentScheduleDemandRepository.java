package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleDemand;

import java.time.LocalDate;
import java.util.List;

public interface DepartmentScheduleDemandRepository {

    List<DepartmentScheduleDemand> listByDepartmentAndDateRange(Long departmentId, LocalDate startDate, LocalDate endDate);
}
