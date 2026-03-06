package me.jianwen.mediask.api.response.authz;

import lombok.Data;

@Data
public class PermissionResponse {

    private Long permissionId;
    private String permissionCode;
    private String permissionName;
    private String description;
}
