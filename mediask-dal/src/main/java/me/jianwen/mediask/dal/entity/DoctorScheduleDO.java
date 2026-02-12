package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.ScheduleStatusEnum;
import me.jianwen.mediask.dal.enums.TimePeriodEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 医生排班实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("doctor_schedules")
public class DoctorScheduleDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 排班日期
     */
    private LocalDate scheduleDate;

    /**
     * 时段
     */
    private TimePeriodEnum timePeriod;

    /**
     * 总号源数
     */
    private Integer totalSlots;

    /**
     * 剩余号源
     */
    private Integer availableSlots;

    /**
     * 状态
     */
    private ScheduleStatusEnum status;

    /**
     * 时段开始时间
     */
    private LocalTime periodStartTime;

    /**
     * 时段结束时间
     */
    private LocalTime periodEndTime;

    /**
     * 单个号源时长（分钟）
     */
    private Integer slotDurationMinutes;

    /**
     * 挂号费
     */
    private BigDecimal fee;

    /**
     * 数据来源类型（TEMPLATE/MANUAL）
     */
    private String sourceType;

    /**
     * 数据来源ID
     */
    private Long sourceId;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
