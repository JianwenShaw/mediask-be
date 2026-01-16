package me.jianwen.mediask.user.application.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户注册请求
 */
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String phone;
    private Integer userType;
    private String realName;
    private Integer gender;
    private LocalDate birthDate;
    private String avatarUrl;
}

