package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.optimization.model.DoctorAvailabilityRule;

import java.util.List;

public interface DoctorAvailabilityRuleRepository {

    List<DoctorAvailabilityRule> listByDoctorIds(List<Long> doctorIds);
}
