package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.StatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医院实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("hospitals")
public class HospitalDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 医院编码
     */
    private String hospitalCode;

    /**
     * 医院等级
     */
    private String hospitalLevel;

    /**
     * 地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String contactPhone;

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
