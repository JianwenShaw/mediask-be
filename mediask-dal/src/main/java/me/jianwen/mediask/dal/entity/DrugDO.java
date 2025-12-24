package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.StatusEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 药品字典实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("drugs")
public class DrugDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 药品编码
     */
    private String drugCode;

    /**
     * 通用名
     */
    private String drugName;

    /**
     * 商品名
     */
    private String tradeName;

    /**
     * 规格
     */
    private String spec;

    /**
     * 单位
     */
    private String unit;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 禁忌症(JSON)
     */
    private String contraindication;

    /**
     * 配伍禁忌(JSON)
     */
    private String interaction;

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
