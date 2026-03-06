package me.jianwen.mediask.api.response.schedule;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ScheduleTemplateResponse {

    private Long id;
    private Long doctorId;
    private String templateName;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;
    private Integer cancelDeadlineMinutes;
    private BigDecimal defaultFee;
    private Integer status;
    private List<ScheduleTemplateRuleResponse> rules;
}
