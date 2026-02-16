package me.jianwen.mediask.schedule.domain.statemachine;

public record TransitionResult<S, E>(
        S fromState,
        S toState,
        E event,
        boolean stateChanged,
        boolean idempotent,
        String reason
) {
}
