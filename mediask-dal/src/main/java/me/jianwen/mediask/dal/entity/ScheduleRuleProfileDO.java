package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schedule_rule_profile")
public class ScheduleRuleProfileDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long departmentId;
    private String profileCode;
    private String profileName;
    private Integer versionNo;
    private String profileStatus;
    private String constraintDslJson;
    private String description;
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
