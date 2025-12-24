package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 病历版本历史实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("medical_record_versions")
public class MedicalRecordVersionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联病历ID
     */
    private Long recordId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 病历完整快照(JSON)
     */
    private String snapshot;

    /**
     * 修改人用户ID
     */
    private Long modifiedBy;

    /**
     * 修改原因(医疗合规要求)
     */
    private String modifyReason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
