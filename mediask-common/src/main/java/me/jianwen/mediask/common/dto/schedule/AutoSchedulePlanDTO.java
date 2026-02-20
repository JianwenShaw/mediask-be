package me.jianwen.mediask.common.dto.schedule;

import java.util.List;

/**
 * 自动排班结果DTO
 */
public record AutoSchedulePlanDTO(
        String planId,
        List<Long> generatedScheduleIds,
        ScoreSummary scoreSummary,
        List<AssignmentExplanation> explanations,
        List<UnfilledSlot> unfilledSlots,
        List<String> minimalConflictSet,
        PlanDiff planDiff,
        List<String> warnings
) {

    public record ScoreSummary(
            double totalScore,
            int hardViolationCount,
            SoftScoreBreakdown softScoreBreakdown
    ) {
    }

    public record SoftScoreBreakdown(
            double fairnessScore,
            double preferenceScore,
            double continuityScore,
            double seniorCoverageScore,
            double weekendBalanceScore
    ) {
    }

    public record AssignmentExplanation(
            String date,
            Integer periodCode,
            Long doctorId,
            List<String> reasons,
            List<String> penalties
    ) {
    }

    public record UnfilledSlot(
            String date,
            Integer periodCode,
            Integer missingDoctors,
            String reason
    ) {
    }

    public record PlanDiff(
            int addedAssignments,
            int removedAssignments,
            int changedSlots,
            int unchangedAssignments,
            List<DiffSlot> changedSlotDetails
    ) {
    }

    public record DiffSlot(
            String changeType,
            String date,
            Integer periodCode,
            List<Long> beforeDoctorIds,
            List<Long> afterDoctorIds
    ) {
    }
}
