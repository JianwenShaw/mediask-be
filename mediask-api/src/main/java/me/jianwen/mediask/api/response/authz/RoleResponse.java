package me.jianwen.mediask.api.response.authz;

import lombok.Data;

import java.util.List;

@Data
public class RoleResponse {

    private Long roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private List<String> permissionCodes;
}
