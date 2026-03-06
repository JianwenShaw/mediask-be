package me.jianwen.mediask.schedule.domain.repository;

import me.jianwen.mediask.schedule.domain.plan.SchedulePlan;
import me.jianwen.mediask.schedule.domain.plan.SchedulePlanConstraintSnapshot;
import me.jianwen.mediask.schedule.domain.plan.SchedulePlanItem;

import java.util.List;
import java.util.Optional;

public interface SchedulePlanRepository {

    int findLatestVersion(String planCode);

    Long savePlan(SchedulePlan plan);

    void savePlanItems(Long planId, List<SchedulePlanItem> items);

    void saveConstraintSnapshots(Long planId, List<SchedulePlanConstraintSnapshot> snapshots);

    Optional<SchedulePlan> findById(Long planId);

    List<SchedulePlan> listByPlanCode(String planCode);

    List<SchedulePlanItem> listPlanItems(Long planId);

    void updatePlanStatus(Long planId, String status);

    void archivePublishedPlans(String planCode);
}
