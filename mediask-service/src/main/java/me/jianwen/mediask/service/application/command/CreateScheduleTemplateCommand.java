package me.jianwen.mediask.service.application.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateScheduleTemplateCommand {

    private Long doctorId;
    private String templateName;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private Integer cancelDeadlineMinutes;
    private BigDecimal defaultFee;
    private List<ScheduleTemplateRuleCommand> rules;
}
