package me.jianwen.mediask.service.application.command;

import lombok.Builder;
import lombok.Data;

/**
 * 用户登录命令
 */
@Data
@Builder
public class LoginCommand {

    private String account;

    private String password;

    private String clientIp;
}
