package me.jianwen.mediask.api.model.schedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GenerateScheduleFromTemplateRequest {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;
}
