package me.jianwen.mediask.user.application.request;

import lombok.Data;

/**
 * 用户登录请求
 */
@Data
public class LoginRequest {
    private String account; // 可填写用户名或手机号
    private String password;
}

