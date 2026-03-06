package me.jianwen.mediask.infra.schedule.engine.dsl;

import me.jianwen.mediask.common.util.JsonUtil;
import me.jianwen.mediask.schedule.domain.optimization.DepartmentScheduleOptimizationRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于旧参数自动生成默认 JSON DSL。
 */
public final class DefaultConstraintDslFactory {

    private DefaultConstraintDslFactory() {
    }

    public static String build(DepartmentScheduleOptimizationRequest request) {
        var hard = request.hardConstraints();
        var soft = request.softGoals();

        List<Map<String, Object>> rules = new ArrayList<>();
        rules.add(rule("H_AVAILABILITY", "HARD", "DOCTOR_AVAILABILITY", 1D, Map.of()));
        rules.add(rule("H_TIME_OFF", "HARD", "DOCTOR_TIME_OFF", 1D, Map.of()));
        rules.add(rule("H_MAX_CONSECUTIVE_DAYS", "HARD", "MAX_CONSECUTIVE_DAYS", 1D,
                Map.of("maxDays", hard.maxConsecutiveDays())));
        rules.add(rule("H_MAX_SHIFTS_PER_WEEK", "HARD", "MAX_SHIFTS_PER_WEEK", 1D,
                Map.of("maxShifts", hard.maxShiftsPerWeek())));
        rules.add(rule("H_MIN_REST_HOURS", "HARD", "MIN_REST_HOURS", 1D,
                Map.of("hours", hard.minRestHoursBetweenShifts())));
        rules.add(rule("H_HOLIDAY_POLICY", "HARD", "HOLIDAY_POLICY", 1D,
                Map.of(
                        "excludeHolidays", hard.excludeHolidays(),
                        "policy", hard.holidayPolicy(),
                        "reductionFactor", hard.holidayReductionFactor()
                )));

        rules.add(rule("S_FAIRNESS", "SOFT", "FAIRNESS", soft.fairnessWeight(), Map.of()));
        rules.add(rule("S_PREFERENCE", "SOFT", "PREFERENCE", soft.preferenceWeight(), Map.of()));
        rules.add(rule("S_CONTINUITY", "SOFT", "CONTINUITY", soft.continuityWeight(), Map.of()));
        rules.add(rule("S_SENIOR_COVERAGE", "SOFT", "SENIOR_COVERAGE", soft.seniorCoverageWeight(), Map.of()));
        rules.add(rule("S_WEEKEND_BALANCE", "SOFT", "WEEKEND_BALANCE", soft.weekendBalanceWeight(), Map.of()));

        List<String> hardRuleIds = List.of(
                "H_AVAILABILITY",
                "H_TIME_OFF",
                "H_MAX_CONSECUTIVE_DAYS",
                "H_MAX_SHIFTS_PER_WEEK",
                "H_MIN_REST_HOURS",
                "H_HOLIDAY_POLICY"
        );
        List<String> softRuleIds = List.of(
                "S_FAIRNESS",
                "S_PREFERENCE",
                "S_CONTINUITY",
                "S_SENIOR_COVERAGE",
                "S_WEEKEND_BALANCE"
        );

        Map<String, Object> objective = new LinkedHashMap<>();
        objective.put("type", "WEIGHTED_SUM");
        objective.put("weights", Map.of(
                "fairness", soft.fairnessWeight(),
                "preference", soft.preferenceWeight(),
                "continuity", soft.continuityWeight(),
                "seniorCoverage", soft.seniorCoverageWeight(),
                "weekendBalance", soft.weekendBalanceWeight()
        ));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.0");
        root.put("rules", rules);
        root.put("hardExpr", andExpr(hardRuleIds));
        root.put("softExpr", andExpr(softRuleIds));
        root.put("objective", objective);
        return JsonUtil.toJson(root);
    }

    private static Map<String, Object> rule(
            String id,
            String kind,
            String type,
            double weight,
            Map<String, Object> params) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", id);
        rule.put("kind", kind);
        rule.put("type", type);
        rule.put("enabled", true);
        rule.put("weight", weight);
        rule.put("priority", 0);
        rule.put("params", params);
        return rule;
    }

    private static Map<String, Object> andExpr(List<String> ruleIds) {
        Map<String, Object> expr = new LinkedHashMap<>();
        expr.put("op", "AND");
        List<Map<String, Object>> children = new ArrayList<>();
        for (String ruleId : ruleIds) {
            children.add(Map.of("ref", ruleId));
        }
        expr.put("children", children);
        return expr;
    }
}
