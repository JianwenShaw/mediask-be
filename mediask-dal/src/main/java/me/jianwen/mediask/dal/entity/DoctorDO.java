package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.StatusEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 医生实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("doctors")
public class DoctorDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 所属医院ID(冗余字段,便于查询)
     */
    private Long hospitalId;

    /**
     * 所属科室ID
     */
    private Long deptId;

    /**
     * 医生编码
     */
    private String doctorCode;

    /**
     * 职称
     */
    private String title;

    /**
     * 擅长领域(JSON数组)
     */
    private String specialty;

    /**
     * 个人简介
     */
    private String introduction;

    /**
     * 诊疗费用
     */
    private BigDecimal consultationFee;

    /**
     * 执业证书号
     */
    private String licenseNumber;

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

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
