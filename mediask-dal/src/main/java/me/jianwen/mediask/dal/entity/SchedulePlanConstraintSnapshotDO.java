package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schedule_plan_constraint_snapshot")
public class SchedulePlanConstraintSnapshotDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long planId;
    private String snapshotType;
    private String snapshotJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
