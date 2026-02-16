package me.jianwen.mediask.schedule.domain.statemachine;

public record StateTransition<S, E, C>(
        S fromState,
        E event,
        S toState,
        TransitionGuard<C> guard,
        TransitionAction<C> action
) {
}
