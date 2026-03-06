package me.jianwen.mediask.service.application.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PermissionDTO {

    private Long permissionId;
    private String permissionCode;
    private String permissionName;
    private String description;
}
