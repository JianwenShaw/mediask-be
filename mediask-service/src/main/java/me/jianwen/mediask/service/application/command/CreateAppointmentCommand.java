package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 创建预约命令
 */
@Data
public class CreateAppointmentCommand {

    private Long scheduleId;

    private LocalDate apptDate;

    private Integer timePeriodCode;

    private LocalTime apptTime;

    private String chiefComplaint;
}
