package me.jianwen.mediask.schedule.domain.heuristic.engine;

import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintResult;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ConstraintType;
import me.jianwen.mediask.schedule.domain.heuristic.constraint.ScheduleConstraint;
import me.jianwen.mediask.schedule.domain.heuristic.context.SolverContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 约束引擎
 *
 * <p>统一管理和执行约束检查：
 * <ul>
 *   <li>注册/注销约束</li>
 *   <li>批量检查约束</li>
 *   <li>收集约束结果</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
public class ConstraintEngine {

    private final List<ScheduleConstraint> constraints = new ArrayList<>();

    /**
     * 注册约束
     */
    public ConstraintEngine register(ScheduleConstraint constraint) {
        this.constraints.add(constraint);
        log.debug("注册约束: {}", constraint.name());
        return this;
    }

    /**
     * 批量注册约束
     */
    public ConstraintEngine registerAll(ScheduleConstraint... constraints) {
        for (ScheduleConstraint constraint : constraints) {
            register(constraint);
        }
        return this;
    }

    /**
     * 检查所有约束
     */
    public List<ConstraintResult> checkAll(SolverContext context) {
        List<ConstraintResult> results = new ArrayList<>();

        for (ScheduleConstraint constraint : constraints) {
            try {
                ConstraintResult result = constraint.check(context);
                results.add(result);

                if (!result.satisfied()) {
                    log.debug("约束 {} 未满足: {}", constraint.name(), result.message());
                }
            } catch (Exception e) {
                log.error("检查约束 {} 时出错", constraint.name(), e);
                results.add(ConstraintResult.fail(
                        "约束检查异常: " + e.getMessage(),
                        1.0,
                        Map.of("constraint", constraint.name())
                ));
            }
        }

        return results;
    }

    /**
     * 只检查硬约束
     */
    public List<ConstraintResult> checkHardConstraints(SolverContext context) {
        return constraints.stream()
                .filter(c -> c.type() == ConstraintType.HARD)
                .map(c -> {
                    try {
                        return c.check(context);
                    } catch (Exception e) {
                        return ConstraintResult.fail("检查异常: " + e.getMessage());
                    }
                })
                .toList();
    }

    /**
     * 只检查软约束
     */
    public List<ConstraintResult> checkSoftConstraints(SolverContext context) {
        return constraints.stream()
                .filter(c -> c.type() == ConstraintType.SOFT)
                .map(c -> {
                    try {
                        return c.check(context);
                    } catch (Exception e) {
                        return ConstraintResult.fail("检查异常: " + e.getMessage());
                    }
                })
                .toList();
    }

    /**
     * 快速检查硬约束（遇到失败立即返回）
     */
    public Optional<ConstraintResult> checkHardConstraintsFast(SolverContext context) {
        for (ScheduleConstraint constraint : constraints) {
            if (constraint.type() != ConstraintType.HARD) continue;

            try {
                ConstraintResult result = constraint.check(context);
                if (!result.satisfied()) {
                    return Optional.of(result);
                }
            } catch (Exception e) {
                log.error("检查约束 {} 时出错", constraint.name(), e);
                return Optional.of(ConstraintResult.fail("检查异常: " + e.getMessage()));
            }
        }
        return Optional.empty();
    }

    /**
     * 获取所有注册的约束
     */
    public List<ScheduleConstraint> getConstraints() {
        return List.copyOf(constraints);
    }

    /**
     * 获取约束数量
     */
    public int getConstraintCount() {
        return constraints.size();
    }

    /**
     * 获取硬约束数量
     */
    public int getHardConstraintCount() {
        return (int) constraints.stream()
                .filter(c -> c.type() == ConstraintType.HARD)
                .count();
    }

    /**
     * 获取软约束数量
     */
    public int getSoftConstraintCount() {
        return (int) constraints.stream()
                .filter(c -> c.type() == ConstraintType.SOFT)
                .count();
    }

    /**
     * 清空所有约束
     */
    public void clear() {
        constraints.clear();
    }

    /**
     * 创建默认约束引擎（包含所有约束）
     */
    public static ConstraintEngine createDefault() {
        return new ConstraintEngine()
                .registerAll(
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.DoctorAvailabilityConstraint(),
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.ConsecutiveDaysConstraint(),
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.DailyAssignmentLimitConstraint(),
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.HolidayExclusionConstraint(),
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.WorkloadBalanceConstraint(),
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.TimePreferenceConstraint(),
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.ExpertCoverageConstraint(),
                        new me.jianwen.mediask.schedule.domain.heuristic.constraint.impl.GapMinimizationConstraint()
                );
    }
}
