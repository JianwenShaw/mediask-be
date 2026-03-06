package me.jianwen.mediask.service.application.support;

import me.jianwen.mediask.schedule.domain.plan.SchedulePlanItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchedulePlanDiffCalculatorTest {

    @Test
    void shouldCalculatePlanDiffWhenAssignmentsChanged() {
        LocalDate date = LocalDate.of(2026, 2, 20);
        List<SchedulePlanItem> before = List.of(
                new SchedulePlanItem(date, 1, 101L, false, "[]", "[]", null),
                new SchedulePlanItem(date, 2, 102L, false, "[]", "[]", null)
        );
        List<SchedulePlanItem> after = List.of(
                new SchedulePlanItem(date, 1, 103L, false, "[]", "[]", null),
                new SchedulePlanItem(date, 2, 102L, false, "[]", "[]", null),
                new SchedulePlanItem(date, 3, 104L, false, "[]", "[]", null)
        );

        SchedulePlanDiffCalculator.PlanDiffResult result = SchedulePlanDiffCalculator.calculate(before, after);

        assertEquals(2, result.addedAssignments());
        assertEquals(1, result.removedAssignments());
        assertEquals(2, result.changedSlots());
        assertEquals(1, result.unchangedAssignments());
    }
}
