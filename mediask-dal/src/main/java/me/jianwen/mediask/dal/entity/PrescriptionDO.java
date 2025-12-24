package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.PrescriptionStatusEnum;
import me.jianwen.mediask.dal.enums.PrescriptionTypeEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("prescriptions")
public class PrescriptionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 处方号
     */
    private String prescriptionNo;

    /**
     * 病历ID
     */
    private Long recordId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 处方类型
     */
    private PrescriptionTypeEnum prescriptionType;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 状态
     */
    private PrescriptionStatusEnum status;

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
