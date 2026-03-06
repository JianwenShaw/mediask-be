package me.jianwen.mediask.schedule.domain.heuristic.problem;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 排班问题定义
 *
 * <p>封装排班优化的完整问题描述：
 * <ul>
 *   <li>医生集合</li>
 *   <li>排班日期范围</li>
 *   <li>时间配置</li>
 *   <li>优化目标</li>
 *   <li>约束条件</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class ScheduleProblem {

    /**
     * 医生集合
     */
    private DoctorSet doctorSet;

    /**
     * 排班日期范围
     */
    private DateRange dateRange;

    /**
     * 时间配置
     */
    private TimeSlotConfig timeConfig;

    /**
     * 优化目标（key: 目标名称, value: 权重）
     */
    private Map<String, Double> optimizationGoals;

    /**
     * 是否需要专家号覆盖
     */
    @Builder.Default
    private boolean requireExpertCoverage = true;

    /**
     * 专家号最小覆盖率
     */
    @Builder.Default
    private double minExpertCoverageRate = 0.3;

    /**
     * 是否启用负载均衡
     */
    @Builder.Default
    private boolean enableWorkloadBalance = true;

    /**
     * 工作量均衡阈值（标准差上限）
     */
    @Builder.Default
    private double workloadBalanceThreshold = 2.0;

    /**
     * 是否启用时段偏好
     */
    @Builder.Default
    private boolean enableTimePreference = true;

    /**
     * 是否启用空档最小化
     */
    @Builder.Default
    private boolean enableGapMinimization = true;

    /**
     * 是否必须排除节假日
     */
    @Builder.Default
    private boolean excludeHolidays = true;

    /**
     * 连续工作天数上限
     */
    @Builder.Default
    private int maxConsecutiveDays = 5;

    /**
     * 每日排班次数上限
     */
    @Builder.Default
    private int maxDailyAssignments = 3;

    /**
     * 是否强制使用专家
     */
    @Builder.Default
    private boolean forceExpertAssignment = false;

    /**
     * 创建排班问题
     */
    public static ScheduleProblem create(
            DoctorSet doctorSet,
            DateRange dateRange,
            TimeSlotConfig timeConfig) {
        return ScheduleProblem.builder()
                .doctorSet(doctorSet)
                .dateRange(dateRange)
                .timeConfig(timeConfig)
                .build();
    }

    /**
     * 设置优化目标
     */
    public ScheduleProblem withOptimizationGoals(Map<String, Double> goals) {
        this.optimizationGoals = goals;
        return this;
    }

    /**
     * 获取医生列表
     */
    public List<Doctor> getDoctors() {
        return doctorSet.getDoctors();
    }

    /**
     * 获取专家列表
     */
    public List<Doctor> getExperts() {
        return doctorSet.getExperts();
    }

    /**
     * 获取普通医生列表
     */
    public List<Doctor> getRegularDoctors() {
        return doctorSet.getRegularDoctors();
    }

    /**
     * 检查问题规模（用于选择算法）
     *
     * @return 规模等级: SMALL(<50天), MEDIUM(50-200天), LARGE(>200天)
     */
    public String getScaleLevel() {
        long days = dateRange.getTotalDays();
        if (days < 50) {
            return "SMALL";
        } else if (days < 200) {
            return "MEDIUM";
        } else {
            return "LARGE";
        }
    }

    /**
     * 计算估计的排班数量
     */
    public int estimateTotalAssignments() {
        int doctors = doctorSet.size();
        int days = (int) dateRange.getTotalDays();
        int periods = timeConfig.getEnabledPeriods().size();
        return doctors * days * periods / 3; // 假设1/3的组合会被填充
    }
}
