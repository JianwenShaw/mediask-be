package me.jianwen.mediask.schedule.domain.optimization.engine;

import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.rule.DoctorAvailabilityRule;
import me.jianwen.mediask.schedule.domain.rule.DoctorTimeOff;
import me.jianwen.mediask.schedule.domain.optimization.ScheduleDoctorProfile;

import java.util.List;

/**
 * 排班引擎输入。
 */
public record SchedulingEngineRequest(
        String namespace,
        Long departmentId,
        DepartmentScheduleOptimizationRequest optimizationRequest,
        List<ScheduleDoctorProfile> doctors,
        List<DoctorAvailabilityRule> availabilityRules,
        List<DoctorTimeOff> timeOffRules,
        SolverConfig solverConfig,
        String constraintDslJson
) {
}
