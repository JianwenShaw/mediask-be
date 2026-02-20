package me.jianwen.mediask.service.application.command;

import lombok.Data;

/**
 * 创建排班规则配置命令。
 */
@Data
public class CreateScheduleRuleProfileCommand {

    private Long departmentId;
    private String profileCode;
    private String profileName;
    private String constraintDslJson;
    private String description;
    private Long operatorId;
}
