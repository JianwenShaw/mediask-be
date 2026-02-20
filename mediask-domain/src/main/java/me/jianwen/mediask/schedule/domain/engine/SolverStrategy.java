package me.jianwen.mediask.schedule.domain.engine;

import java.util.Locale;

/**
 * 排班求解策略。
 */
public enum SolverStrategy {
    AUTO,
    RULE_GREEDY,
    LOCAL_SEARCH,
    CP_SAT;

    public static SolverStrategy from(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        try {
            return SolverStrategy.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AUTO;
        }
    }
}
