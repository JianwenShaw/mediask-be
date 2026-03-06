package me.jianwen.mediask.api.request.schedule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateScheduleTemplateRequest {

    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotNull(message = "生效开始日期不能为空")
    private LocalDate effectiveStartDate;

    @NotNull(message = "生效结束日期不能为空")
    private LocalDate effectiveEndDate;

    @NotNull(message = "取消截止时间不能为空")
    private Integer cancelDeadlineMinutes;

    @NotNull(message = "默认挂号费不能为空")
    private BigDecimal defaultFee;

    @Valid
    @NotEmpty(message = "模板规则不能为空")
    private List<ScheduleTemplateRuleRequest> rules;
}
