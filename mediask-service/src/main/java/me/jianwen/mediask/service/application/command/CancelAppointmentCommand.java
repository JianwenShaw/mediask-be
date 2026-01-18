package me.jianwen.mediask.service.application.command;

import lombok.Data;

/**
 * 取消预约命令
 */
@Data
public class CancelAppointmentCommand {

    private Long appointmentId;

    private String reason;

    private Long operatorId;

    private Integer operatorType;
}
