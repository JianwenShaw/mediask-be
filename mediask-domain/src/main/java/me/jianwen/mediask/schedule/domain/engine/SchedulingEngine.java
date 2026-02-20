package me.jianwen.mediask.schedule.domain.engine;

/**
 * 排班引擎统一入口。
 */
public interface SchedulingEngine {

    SchedulingEngineResult optimize(SchedulingEngineRequest request);
}
