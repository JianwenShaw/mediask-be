package me.jianwen.mediask.schedule.domain.statemachine;

@FunctionalInterface
public interface TransitionAction<C> {

    void execute(C context);
}
