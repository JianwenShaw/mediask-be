package me.jianwen.mediask.api.model.authz;

import lombok.Data;

@Data
public class PermissionResponse {

    private Long permissionId;
    private String permissionCode;
    private String permissionName;
    private String description;
}
