package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 创建排班命令
 */
@Data
public class CreateScheduleCommand {

    private Long doctorId;

    private LocalDate scheduleDate;

    private Integer timePeriodCode;

    private Integer totalSlots;

    private Integer slotDurationMinutes;
}
