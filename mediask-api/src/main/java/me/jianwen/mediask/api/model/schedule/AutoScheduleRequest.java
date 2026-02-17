package me.jianwen.mediask.api.model.schedule;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * 自动排班请求（API层）
 */
@Data
public class AutoScheduleRequest {

    /**
     * 科室ID
     */
    @NotNull(message = "科室ID不能为空")
    private Long departmentId;

    /**
     * 日期范围
     */
    @NotNull(message = "日期范围不能为空")
    private DateRangeRequest dateRange;

    /**
     * 指定参与排班的医生列表（为空则取科室全部在职医生）
     */
    private List<Long> doctorIds;

    /**
     * 时段列表（1上午 2下午 3晚上）
     */
    @NotNull(message = "时段列表不能为空")
    private List<Integer> periods;

    /**
     * 科室需求（为空则按默认需求生成）
     */
    private DemandRequest demand;

    /**
     * 硬约束
     */
    private HardConstraintsRequest hardConstraints;

    /**
     * 软约束权重
     */
    private SoftGoalsRequest softGoals;

    /**
     * 求解器配置
     */
    private SolverConfigRequest solverConfig;

    @Data
    public static class DateRangeRequest {

        @NotNull(message = "开始日期不能为空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;

        @NotNull(message = "结束日期不能为空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
    }

    @Data
    public static class DemandRequest {

        private List<DemandItemRequest> byDatePeriod;
    }

    @Data
    public static class DemandItemRequest {

        @NotNull(message = "需求日期不能为空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate date;

        @NotNull(message = "需求时段不能为空")
        private Integer periodCode;

        @NotNull(message = "最少排班医生数不能为空")
        @Positive(message = "最少排班医生数必须大于0")
        private Integer requiredDoctors;

        @PositiveOrZero(message = "最少资深医生数不能小于0")
        private Integer minSeniorDoctors;
    }

    @Data
    public static class HardConstraintsRequest {

        @Positive(message = "最大连续出诊天数必须大于0")
        private Integer maxConsecutiveDays;

        @Positive(message = "每周最大班次数必须大于0")
        private Integer maxShiftsPerWeek;

        @Positive(message = "班次间最少休息小时必须大于0")
        private Integer minRestHoursBetweenShifts;

        /**
         * 兼容旧逻辑：是否节假日全停
         */
        private Boolean excludeHolidays;

        /**
         * 节假日策略：CLOSE/REDUCED/NORMAL
         */
        private String holidayPolicy;

        /**
         * 节假日降载系数，仅在 REDUCED 策略下生效（0,1]
         */
        @DecimalMin(value = "0.01", message = "节假日降载系数必须大于0")
        @DecimalMax(value = "1.00", message = "节假日降载系数必须小于等于1")
        private Double holidayReductionFactor;
    }

    @Data
    public static class SoftGoalsRequest {

        private Double fairnessWeight;
        private Double preferenceWeight;
        private Double continuityWeight;
        private Double seniorCoverageWeight;
        private Double weekendBalanceWeight;
    }

    @Data
    public static class SolverConfigRequest {

        private String strategy;
        private Integer maxIterations;
        private Long timeLimitMs;
        private Long seed;
    }
}
