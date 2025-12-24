package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.StatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 科室实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("departments")
public class DepartmentDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 医院ID
     */
    private Long hospitalId;

    /**
     * 科室编码
     */
    private String deptCode;

    /**
     * 科室名称
     */
    private String deptName;

    /**
     * 科室简介
     */
    private String deptIntro;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

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
