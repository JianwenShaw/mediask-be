package me.jianwen.mediask.service.application.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户注册命令
 */
@Data
@Builder
public class RegisterCommand {

    private String username;

    private String password;

    private String phone;

    private Integer userType;

    private String realName;

    private Integer gender;

    private LocalDate birthDate;

    private String avatarUrl;
}
