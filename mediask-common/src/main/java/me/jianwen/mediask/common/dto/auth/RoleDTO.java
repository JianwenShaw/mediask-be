package me.jianwen.mediask.common.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleDTO {

    private Long roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private List<String> permissionCodes;
}
