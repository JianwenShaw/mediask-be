package me.jianwen.mediask.api.request.authz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRolesRequest {

    @NotEmpty(message = "角色编码不能为空")
    private List<@NotBlank(message = "角色编码不能为空白") String> roleCodes;
}
