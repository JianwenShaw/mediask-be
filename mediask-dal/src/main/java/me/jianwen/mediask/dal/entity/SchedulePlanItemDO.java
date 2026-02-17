package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("schedule_plan_items")
public class SchedulePlanItemDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long planId;
    private LocalDate scheduleDate;
    private Integer periodCode;
    private Long doctorId;
    private Integer isSenior;
    private String reasonJson;
    private String penaltyJson;
    private BigDecimal scoreDelta;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
