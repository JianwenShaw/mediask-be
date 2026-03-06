package me.jianwen.mediask.schedule.domain.optimization.engine.constraint;

import java.util.Map;

/**
 * 目标函数声明。
 */
public record ObjectiveSpec(
        String type,
        Map<String, Double> weights
) {
}
