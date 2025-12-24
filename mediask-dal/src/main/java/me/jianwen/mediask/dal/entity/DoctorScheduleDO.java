package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.StatusEnum;
import me.jianwen.mediask.dal.enums.TimePeriodEnum;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private StatusEnum status;

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
}
