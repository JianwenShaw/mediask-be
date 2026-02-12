package me.jianwen.mediask.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("appointment_events")
public class AppointmentEventDO implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long appointmentId;
    private String eventType;
    private Integer fromStatus;
    private Integer toStatus;
    private String operatorType;
    private Long operatorId;
    private String payloadJson;
    private LocalDateTime occurredAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;
}
