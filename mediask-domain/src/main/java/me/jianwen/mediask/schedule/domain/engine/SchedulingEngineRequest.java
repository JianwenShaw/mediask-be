package me.jianwen.mediask.schedule.domain.engine;

import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.optimization.model.DoctorAvailabilityRule;
import me.jianwen.mediask.schedule.domain.optimization.model.DoctorTimeOff;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleDoctorProfile;

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
