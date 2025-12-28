package me.jianwen.mediask.api.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 注册请求
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64之间")
    private String password;

    @Size(max = 20, message = "手机号长度不能超过20")
    private String phone;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;

    @Size(max = 64, message = "真实姓名长度不能超过64")
    private String realName;

    private Integer gender;

    private LocalDate birthDate;

    @Size(max = 255, message = "头像地址过长")
    private String avatarUrl;
}

