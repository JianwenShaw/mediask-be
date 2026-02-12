package me.jianwen.mediask.dal.entity;

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
@TableName("schedule_templates")
public class ScheduleTemplateDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long doctorId;
    private String templateName;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private Integer cancelDeadlineMinutes;
    private BigDecimal defaultFee;
    private Integer status;
    private Integer version;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
