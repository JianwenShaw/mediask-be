package me.jianwen.mediask.user.domain.entity;

public record PermissionInfo(
        Long permissionId,
        String permissionCode,
        String permissionName,
        String description
) {
}
