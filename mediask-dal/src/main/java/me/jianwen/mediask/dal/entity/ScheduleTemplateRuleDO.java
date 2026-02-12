package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("schedule_template_rules")
public class ScheduleTemplateRuleDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long templateId;
    private Integer weekday;
    private Integer timePeriod;
    private LocalTime periodStartTime;
    private LocalTime periodEndTime;
    private Integer slotDurationMinutes;
    private Integer slotCapacity;
    private BigDecimal fee;
    private Integer status;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
