package me.jianwen.mediask.user.domain.entity;

import java.util.List;

public record RoleInfo(
        Long roleId,
        String roleCode,
        String roleName,
        String description,
        List<String> permissionCodes
) {
}
