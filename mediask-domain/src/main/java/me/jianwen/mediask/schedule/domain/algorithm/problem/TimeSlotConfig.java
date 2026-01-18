package me.jianwen.mediask.schedule.domain.algorithm.problem;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;

/**
 * 时间配置
 *
 * <p>定义排班的时间槽配置：
 * <ul>
 *   <li>时段定义（上午、下午、晚上）</li>
 *   <li>每个时段的最大号源数</li>
 *   <li>每个号源的时长</li>
 * </ul>
 *
 * @author MediAsk
 */
@Data
@Builder
public class TimeSlotConfig {

    /**
     * 上午时段开始时间
     */
    @Builder.Default
    private LocalTime morningStart = LocalTime.of(8, 0);

    /**
     * 上午时段结束时间
     */
    @Builder.Default
    private LocalTime morningEnd = LocalTime.of(12, 0);

    /**
     * 下午时段开始时间
     */
    @Builder.Default
    private LocalTime afternoonStart = LocalTime.of(14, 0);

    /**
     * 下午时段结束时间
     */
    @Builder.Default
    private LocalTime afternoonEnd = LocalTime.of(18, 0);

    /**
     * 晚上时段开始时间
     */
    @Builder.Default
    private LocalTime eveningStart = LocalTime.of(19, 0);

    /**
     * 晚上时段结束时间
     */
    @Builder.Default
    private LocalTime eveningEnd = LocalTime.of(21, 0);

    /**
     * 每个号源的时长（分钟）
     */
    @Builder.Default
    private int slotDurationMinutes = 30;

    /**
     * 上午最大号源数
     */
    @Builder.Default
    private int morningMaxSlots = 20;

    /**
     * 下午最大号源数
     */
    @Builder.Default
    private int afternoonMaxSlots = 20;

    /**
     * 晚上最大号源数
     */
    @Builder.Default
    private int eveningMaxSlots = 10;

    /**
     * 启用的时段
     */
    @Builder.Default
    private Set<String> enabledPeriods = Set.of("MORNING", "AFTERNOON");

    /**
     * 时段名称常量
     */
    public static final String MORNING = "MORNING";
    public static final String AFTERNOON = "AFTERNOON";
    public static final String EVENING = "EVENING";

    /**
     * 检查时段是否启用
     */
    public boolean isPeriodEnabled(String period) {
        return enabledPeriods.contains(period.toUpperCase());
    }

    /**
     * 获取时段的最大号源数
     */
    public int getMaxSlotsForPeriod(String period) {
        return switch (period.toUpperCase()) {
            case MORNING -> morningMaxSlots;
            case AFTERNOON -> afternoonMaxSlots;
            case EVENING -> eveningMaxSlots;
            default -> throw new IllegalArgumentException("Unknown period: " + period);
        };
    }

    /**
     * 计算时段内的时间片数量
     */
    public int calculateSlotsCount(String period) {
        LocalTime start = getPeriodStart(period);
        LocalTime end = getPeriodEnd(period);
        int totalMinutes = (end.getHour() * 60 + end.getMinute())
                - (start.getHour() * 60 + start.getMinute());
        return totalMinutes / slotDurationMinutes;
    }

    /**
     * 获取时段开始时间
     */
    public LocalTime getPeriodStart(String period) {
        return switch (period.toUpperCase()) {
            case MORNING -> morningStart;
            case AFTERNOON -> afternoonStart;
            case EVENING -> eveningStart;
            default -> throw new IllegalArgumentException("Unknown period: " + period);
        };
    }

    /**
     * 获取时段结束时间
     */
    public LocalTime getPeriodEnd(String period) {
        return switch (period.toUpperCase()) {
            case MORNING -> morningEnd;
            case AFTERNOON -> afternoonEnd;
            case EVENING -> eveningEnd;
            default -> throw new IllegalArgumentException("Unknown period: " + period);
        };
    }

    /**
     * 获取所有启用的时段
     */
    public Set<String> getEnabledPeriods() {
        return enabledPeriods;
    }

    /**
     * 默认配置
     */
    public static TimeSlotConfig defaultConfig() {
        return TimeSlotConfig.builder().build();
    }
}
