package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import me.jianwen.mediask.dal.enums.RecordStatusEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 电子病历实体
 *
 * @author jianwen
 * @date 2025-12-17
 */
@Data
@TableName("medical_records")
public class MedicalRecordDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 病历号
     */
    private String recordNo;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 医生ID
     */
    private Long doctorId;

    /**
     * 挂号ID
     */
    private Long apptId;

    /**
     * 主诉
     */
    private String chiefComplaint;

    /**
     * 现病史
     */
    private String presentIllness;

    /**
     * 既往史
     */
    private String pastHistory;

    /**
     * 体格检查
     */
    private String physicalExam;

    /**
     * 诊断(JSON数组)
     */
    private String diagnosis;

    /**
     * 治疗方案
     */
    private String treatmentPlan;

    /**
     * 病历状态
     */
    private RecordStatusEnum recordStatus;

    /**
     * 版本号(每次修改递增)
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
