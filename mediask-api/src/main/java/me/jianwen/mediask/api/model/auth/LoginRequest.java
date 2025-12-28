package me.jianwen.mediask.api.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 64, message = "账号长度不能超过64")
    private String account; // 可填写用户名或手机号

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64之间")
    private String password;
}

