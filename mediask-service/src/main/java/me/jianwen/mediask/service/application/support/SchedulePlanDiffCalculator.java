package me.jianwen.mediask.service.application.support;

import me.jianwen.mediask.schedule.domain.optimization.model.SchedulePlanItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 方案差异计算器。
 */
public final class SchedulePlanDiffCalculator {

    private SchedulePlanDiffCalculator() {
    }

    public static PlanDiffResult calculate(List<SchedulePlanItem> before, List<SchedulePlanItem> after) {
        List<SchedulePlanItem> safeBefore = before == null ? List.of() : before;
        List<SchedulePlanItem> safeAfter = after == null ? List.of() : after;

        Set<ItemKey> beforeKeys = toItemKeySet(safeBefore);
        Set<ItemKey> afterKeys = toItemKeySet(safeAfter);

        int unchangedAssignments = 0;
        for (ItemKey key : beforeKeys) {
            if (afterKeys.contains(key)) {
                unchangedAssignments++;
            }
        }

        int addedAssignments = Math.max(0, afterKeys.size() - unchangedAssignments);
        int removedAssignments = Math.max(0, beforeKeys.size() - unchangedAssignments);

        Map<SlotKey, Set<Long>> beforeSlotMap = toSlotDoctorsMap(safeBefore);
        Map<SlotKey, Set<Long>> afterSlotMap = toSlotDoctorsMap(safeAfter);
        Set<SlotKey> allSlots = new HashSet<>();
        allSlots.addAll(beforeSlotMap.keySet());
        allSlots.addAll(afterSlotMap.keySet());

        int changedSlots = 0;
        List<DiffSlot> changedDetails = new ArrayList<>();
        for (SlotKey slotKey : allSlots.stream().sorted().toList()) {
            Set<Long> oldDoctors = beforeSlotMap.getOrDefault(slotKey, Set.of());
            Set<Long> newDoctors = afterSlotMap.getOrDefault(slotKey, Set.of());
            if (oldDoctors.equals(newDoctors)) {
                continue;
            }
            changedSlots++;
            String changeType = oldDoctors.isEmpty() ? "ADDED"
                    : newDoctors.isEmpty() ? "REMOVED"
                    : "CHANGED";
            changedDetails.add(new DiffSlot(
                    changeType,
                    slotKey.date(),
                    slotKey.periodCode(),
                    oldDoctors.stream().sorted().toList(),
                    newDoctors.stream().sorted().toList()
            ));
        }

        return new PlanDiffResult(
                addedAssignments,
                removedAssignments,
                changedSlots,
                unchangedAssignments,
                changedDetails
        );
    }

    private static Set<ItemKey> toItemKeySet(List<SchedulePlanItem> items) {
        Set<ItemKey> keys = new HashSet<>();
        for (SchedulePlanItem item : items) {
            keys.add(new ItemKey(item.scheduleDate(), item.periodCode(), item.doctorId()));
        }
        return keys;
    }

    private static Map<SlotKey, Set<Long>> toSlotDoctorsMap(List<SchedulePlanItem> items) {
        Map<SlotKey, Set<Long>> result = new HashMap<>();
        for (SchedulePlanItem item : items) {
            result.computeIfAbsent(new SlotKey(item.scheduleDate(), item.periodCode()), ignored -> new HashSet<>())
                    .add(item.doctorId());
        }
        return result;
    }

    public record PlanDiffResult(
            int addedAssignments,
            int removedAssignments,
            int changedSlots,
            int unchangedAssignments,
            List<DiffSlot> changedDetails
    ) {
    }

    public record DiffSlot(
            String changeType,
            LocalDate date,
            Integer periodCode,
            List<Long> beforeDoctorIds,
            List<Long> afterDoctorIds
    ) {
    }

    private record ItemKey(LocalDate date, Integer periodCode, Long doctorId) {
    }

    private record SlotKey(LocalDate date, Integer periodCode) implements Comparable<SlotKey> {
        @Override
        public int compareTo(SlotKey other) {
            int dateCompare = this.date.compareTo(other.date);
            if (dateCompare != 0) {
                return dateCompare;
            }
            return Comparator.nullsLast(Integer::compareTo).compare(this.periodCode, other.periodCode);
        }
    }
}
