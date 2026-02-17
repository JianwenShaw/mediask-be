package me.jianwen.mediask.schedule.domain.service;

import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleDemand;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationRequest;
import me.jianwen.mediask.schedule.domain.optimization.model.DepartmentScheduleOptimizationResult;
import me.jianwen.mediask.schedule.domain.optimization.model.ScheduleDoctorProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentScheduleOptimizationDomainServiceTest {

    private final DepartmentScheduleOptimizationDomainService service = new DepartmentScheduleOptimizationDomainService();
    private final LocalDate date = LocalDate.of(2026, 2, 17);

    @Test
    void shouldSkipHolidayWhenPolicyIsClose() {
        DepartmentScheduleOptimizationResult result = service.optimize(
                request("CLOSE", 0.5D, 2, Set.of(date), Set.of()),
                doctors(),
                List.of(),
                List.of()
        );

        assertTrue(result.assignments().isEmpty());
        assertTrue(result.unfilledSlots().isEmpty());
        assertEquals(0, result.hardViolationCount());
    }

    @Test
    void shouldReduceDemandOnHolidayWhenPolicyIsReduced() {
        DepartmentScheduleOptimizationResult result = service.optimize(
                request("REDUCED", 0.5D, 2, Set.of(date), Set.of()),
                doctors(),
                List.of(),
                List.of()
        );

        assertEquals(1, result.assignments().size());
        assertTrue(result.unfilledSlots().isEmpty());
        assertEquals(0, result.hardViolationCount());
    }

    @Test
    void shouldKeepNormalDemandOnHolidayWhenPolicyIsNormal() {
        DepartmentScheduleOptimizationResult result = service.optimize(
                request("NORMAL", 0.5D, 2, Set.of(date), Set.of()),
                doctors(),
                List.of(),
                List.of()
        );

        assertEquals(1, result.assignments().size());
        assertEquals(1, result.hardViolationCount());
    }

    @Test
    void shouldNotApplyHolidayPolicyOnMakeupWorkday() {
        DepartmentScheduleOptimizationResult result = service.optimize(
                request("CLOSE", 0.5D, 1, Set.of(date), Set.of(date)),
                doctors(),
                List.of(),
                List.of()
        );

        assertEquals(1, result.assignments().size());
        assertEquals(0, result.hardViolationCount());
    }

    private List<ScheduleDoctorProfile> doctors() {
        return List.of(new ScheduleDoctorProfile(100L, 200L, "主治医师", false));
    }

    private DepartmentScheduleOptimizationRequest request(
            String holidayPolicy,
            double holidayReductionFactor,
            int requiredDoctors,
            Set<LocalDate> holidayDates,
            Set<LocalDate> makeupWorkdayDates) {
        return new DepartmentScheduleOptimizationRequest(
                date,
                date,
                List.of(1),
                List.of(new DepartmentScheduleDemand(200L, date, 1, requiredDoctors, 0)),
                holidayDates,
                makeupWorkdayDates,
                new DepartmentScheduleOptimizationRequest.HardConstraints(
                        5,
                        10,
                        12,
                        false,
                        holidayPolicy,
                        holidayReductionFactor
                ),
                new DepartmentScheduleOptimizationRequest.SoftGoals(0.3D, 0.2D, 0.15D, 0.2D, 0.15D)
        );
    }
}
