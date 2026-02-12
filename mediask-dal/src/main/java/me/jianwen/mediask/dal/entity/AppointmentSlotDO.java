package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 号源时段实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("appointment_slots")
public class AppointmentSlotDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 时段(如09:00)
     */
    private LocalTime slotTime;

    /**
     * 时段结束时间
     */
    private LocalTime slotEndTime;

    /**
     * 是否占用(0空闲 1占用)
     */
    private Integer isOccupied;

    /**
     * 关联预约ID
     */
    private Long apptId;

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
