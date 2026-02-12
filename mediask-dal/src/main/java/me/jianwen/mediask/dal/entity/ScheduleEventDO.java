package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schedule_events")
public class ScheduleEventDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long scheduleId;
    private String eventType;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorId;
    private String reason;
    private String payloadJson;
    private LocalDateTime occurredAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;
}
