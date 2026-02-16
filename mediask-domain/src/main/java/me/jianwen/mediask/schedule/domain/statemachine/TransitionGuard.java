package me.jianwen.mediask.schedule.domain.statemachine;

@FunctionalInterface
public interface TransitionGuard<C> {

    boolean allow(C context);
}
