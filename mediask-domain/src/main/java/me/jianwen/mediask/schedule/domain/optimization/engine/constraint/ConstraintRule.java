package me.jianwen.mediask.schedule.domain.optimization.engine.constraint;

import java.util.Map;

/**
 * DSL 规则声明。
 */
public record ConstraintRule(
        String id,
        ConstraintKind kind,
        String type,
        boolean enabled,
        double weight,
        int priority,
        Map<String, Object> params
) {
}
