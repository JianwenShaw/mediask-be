package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 处方明细实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("prescription_items")
public class PrescriptionItemDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 处方ID
     */
    private Long prescriptionId;

    /**
     * 药品ID
     */
    private Long drugId;

    /**
     * 药品名称(冗余字段)
     */
    private String drugName;

    /**
     * 规格
     */
    private String spec;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 单位
     */
    private String unit;

    /**
     * 用法
     */
    private String usage;

    /**
     * 频次
     */
    private String frequency;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 小计
     */
    private BigDecimal subtotal;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
