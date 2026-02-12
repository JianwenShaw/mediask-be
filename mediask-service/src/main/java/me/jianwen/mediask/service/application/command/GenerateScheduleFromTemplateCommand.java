package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GenerateScheduleFromTemplateCommand {

    private Long templateId;
    private LocalDate startDate;
    private LocalDate endDate;
}
