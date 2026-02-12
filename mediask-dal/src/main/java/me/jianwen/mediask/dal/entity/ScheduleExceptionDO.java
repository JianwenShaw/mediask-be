package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("schedule_exceptions")
public class ScheduleExceptionDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long doctorId;
    private LocalDate exceptionDate;
    private String actionType;
    private LocalTime overrideStartTime;
    private LocalTime overrideEndTime;
    private Integer overrideCapacity;
    private String reason;
    private Integer status;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private LocalDateTime deletedAt;
}
