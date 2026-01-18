package me.jianwen.mediask.schedule.domain.algorithm.problem;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

/**
 * 排班算法中的医生实体
 *
 * <p>包含医生排班所需的所有信息：
 * <ul>
 *   <li>基本信息（ID、姓名、职称）</li>
 *   <li>可用性（请假、进修等）</li>
 *   <li>偏好（时段偏好、工作量目标）</li>
 *   <li>能力（专家、普通）</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class Doctor {

    /**
     * 医生ID
     */
    private Long id;

    /**
     * 医生姓名
     */
    private String name;

    /**
     * 职称（主任医师/副主任医师/主治医师/住院医师）
     */
    private String title;

    /**
     * 是否为专家
     */
    private boolean expert;

    /**
     * 科室ID
     */
    private Long deptId;

    /**
     * 不可用日期集合（请假、进修等）
     */
    private Set<LocalDate> unavailableDates;

    /**
     * 不可用时段集合
     */
    private Set<String> unavailablePeriods;

    /**
     * 偏好时段（逗号分隔，如 "MORNING,AFTERNOON"）
     */
    private String preferredPeriods;

    /**
     * 最大连续工作天数
     */
    @Builder.Default
    private int maxConsecutiveDays = 5;

    /**
     * 每日最大排班次数
     */
    @Builder.Default
    private int maxDailyAssignments = 3;

    /**
     * 目标工作量（排班次数）
     */
    @Builder.Default
    private int targetWorkload = 10;

    /**
     * 检查医生在指定日期是否可用
     */
    public boolean isAvailableOn(LocalDate date) {
        return unavailableDates == null || !unavailableDates.contains(date);
    }

    /**
     * 检查医生是否可用在指定时段
     */
    public boolean isAvailableInPeriod(String period) {
        return unavailablePeriods == null || !unavailablePeriods.contains(period);
    }

    /**
     * 检查医生是否偏好指定时段
     */
    public boolean prefersPeriod(String period) {
        if (preferredPeriods == null || preferredPeriods.isEmpty()) {
            return true; // 无偏好时默认接受
        }
        String[] prefs = preferredPeriods.split(",");
        for (String p : prefs) {
            if (p.trim().equalsIgnoreCase(period)) {
                return true;
            }
        }
        return false;
    }
}
