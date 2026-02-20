package me.jianwen.mediask.api.model.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建排班规则配置请求。
 */
@Data
public class CreateScheduleRuleProfileRequest {

    @NotNull(message = "科室ID不能为空")
    private Long departmentId;

    @NotBlank(message = "规则编码不能为空")
    private String profileCode;

    @NotBlank(message = "规则名称不能为空")
    private String profileName;

    @NotBlank(message = "约束 DSL 不能为空")
    private String constraintDslJson;

    private String description;
}
