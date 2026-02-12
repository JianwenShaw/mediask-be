package me.jianwen.mediask.schedule.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleTemplate {

    private Long id;
    private Long doctorId;
    private String templateName;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private Integer cancelDeadlineMinutes;
    private BigDecimal defaultFee;
    private Integer status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ScheduleTemplateRule> rules = new ArrayList<>();
}
