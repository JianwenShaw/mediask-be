package me.jianwen.mediask.schedule.domain.optimization.engine.constraint;

import java.util.Map;

/**
 * DSL 编译后的运行时约束模型。
 */
public record CompiledConstraintModel(
        String version,
        String sourceHash,
        Map<String, ConstraintRule> hardRules,
        Map<String, ConstraintRule> softRules,
        ConstraintExpression hardExpression,
        ConstraintExpression softExpression,
        ObjectiveSpec objective
) {
}
