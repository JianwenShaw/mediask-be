package me.jianwen.mediask.schedule.domain.engine.constraint;

import java.util.List;

/**
 * 约束表达式树，支持 AND/OR/NOT 与 RULE_REF。
 */
public sealed interface ConstraintExpression
        permits ConstraintExpression.RuleRef, ConstraintExpression.And, ConstraintExpression.Or, ConstraintExpression.Not {

    record RuleRef(String ruleId) implements ConstraintExpression {
    }

    record And(List<ConstraintExpression> children) implements ConstraintExpression {
    }

    record Or(List<ConstraintExpression> children) implements ConstraintExpression {
    }

    record Not(ConstraintExpression child) implements ConstraintExpression {
    }
}
