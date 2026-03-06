package me.jianwen.mediask.schedule.domain.heuristic.context;

import lombok.Builder;
import lombok.Data;
import me.jianwen.mediask.schedule.domain.heuristic.problem.ScheduleProblem;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 排班算法上下文
 *
 * <p>在算法执行过程中维护状态信息：
 * <ul>
 *   <li>当前已分配的排班记录</li>
 *   <li>每个医生的连续工作天数</li>
 *   <li>每个医生的每日排班次数</li>
 *   <li>每个医生的总工作量</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class SolverContext {

    /**
     * 排班问题定义
     */
    private ScheduleProblem problem;

    /**
     * 已分配的排班记录 (key: date_period, value: doctorId)
     */
    @Builder.Default
    private Map<String, Long> assignments = new HashMap<>();

    /**
     * 每个医生的连续工作天数
     */
    @Builder.Default
    private Map<Long, Integer> consecutiveDays = new HashMap<>();

    /**
     * 每个医生的每日排班次数
     */
    @Builder.Default
    private Map<Long, Map<LocalDate, Integer>> dailyAssignments = new HashMap<>();

    /**
     * 每个医生的总工作量
     */
    @Builder.Default
    private Map<Long, Integer> totalWorkload = new HashMap<>();

    /**
     * 创建新上下文
     */
    public static SolverContext create(ScheduleProblem problem) {
        return SolverContext.builder()
                .problem(problem)
                .assignments(new HashMap<>())
                .consecutiveDays(new HashMap<>())
                .dailyAssignments(new HashMap<>())
                .totalWorkload(new HashMap<>())
                .build();
    }

    /**
     * 添加排班分配
     */
    public void addAssignment(LocalDate date, String period, Long doctorId) {
        String key = generateKey(date, period);
        assignments.put(key, doctorId);

        // 更新连续工作天数
        updateConsecutiveDays(doctorId, date);

        // 更新每日排班次数
        dailyAssignments
                .computeIfAbsent(doctorId, k -> new HashMap<>())
                .merge(date, 1, Integer::sum);

        // 更新总工作量
        totalWorkload.merge(doctorId, 1, Integer::sum);
    }

    /**
     * 生成排班键
     */
    private String generateKey(LocalDate date, String period) {
        return date.toString() + "_" + period;
    }

    /**
     * 更新连续工作天数
     */
    private void updateConsecutiveDays(Long doctorId, LocalDate date) {
        int prevDays = consecutiveDays.getOrDefault(doctorId, 0);
        consecutiveDays.put(doctorId, prevDays + 1);
    }

    /**
     * 获取医生的连续工作天数
     */
    public int getConsecutiveDays(Long doctorId) {
        return consecutiveDays.getOrDefault(doctorId, 0);
    }

    /**
     * 获取医生某天的排班次数
     */
    public int getDailyAssignments(Long doctorId, LocalDate date) {
        Map<LocalDate, Integer> doctorDaily = dailyAssignments.get(doctorId);
        if (doctorDaily == null) {
            return 0;
        }
        return doctorDaily.getOrDefault(date, 0);
    }

    /**
     * 获取医生的总工作量
     */
    public int getTotalWorkload(Long doctorId) {
        return totalWorkload.getOrDefault(doctorId, 0);
    }

    /**
     * 检查是否已分配
     */
    public boolean isAssigned(LocalDate date, String period) {
        String key = generateKey(date, period);
        return assignments.containsKey(key);
    }

    /**
     * 获取指定位置的已分配医生
     */
    public Long getAssignedDoctor(LocalDate date, String period) {
        String key = generateKey(date, period);
        return assignments.get(key);
    }

    /**
     * 复制上下文（用于搜索算法回溯）
     */
    public SolverContext copy() {
        SolverContext copy = SolverContext.builder()
                .problem(this.problem)
                .assignments(new HashMap<>(this.assignments))
                .consecutiveDays(new HashMap<>(this.consecutiveDays))
                .dailyAssignments(new HashMap<>())
                .totalWorkload(new HashMap<>(this.totalWorkload))
                .build();

        // 深拷贝dailyAssignments
        this.dailyAssignments.forEach((doctor, dateMap) -> {
            copy.dailyAssignments.put(doctor, new HashMap<>(dateMap));
        });

        return copy;
    }
}
