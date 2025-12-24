package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.ApptStatusEnum;
import me.jianwen.mediask.dal.enums.TimePeriodEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 挂号预约实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("appointments")
public class AppointmentDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 预约单号
     */
    private String apptNo;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 排班ID
     */
    private Long scheduleId;

    /**
     * 就诊日期
     */
    private LocalDate apptDate;

    /**
     * 时段
     */
    private TimePeriodEnum timePeriod;

    /**
     * 具体时间段
     */
    private LocalTime apptTime;

    /**
     * 挂号状态
     */
    private ApptStatusEnum apptStatus;

    /**
     * 主诉(AI生成)
     */
    private String chiefComplaint;

    /**
     * 挂号费
     */
    private BigDecimal apptFee;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 就诊时间
     */
    private LocalDateTime visitedAt;

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
