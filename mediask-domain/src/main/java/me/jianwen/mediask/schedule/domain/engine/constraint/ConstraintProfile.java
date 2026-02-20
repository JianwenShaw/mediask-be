package me.jianwen.mediask.schedule.domain.engine.constraint;

import java.util.List;

/**
 * JSON DSL 编译前配置结构。
 */
public record ConstraintProfile(
        String version,
        List<ConstraintRule> rules,
        ConstraintExpression hardExpression,
        ConstraintExpression softExpression,
        ObjectiveSpec objective
) {
}
