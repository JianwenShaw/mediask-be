package me.jianwen.mediask.user.domain.repository;

import me.jianwen.mediask.user.domain.entity.PermissionInfo;
import me.jianwen.mediask.user.domain.entity.RoleInfo;

import java.util.List;

/**
 * 鉴权仓储接口
 * 负责用户角色绑定与权限查询
 */
public interface AuthzRepository {

    /**
     * 绑定用户角色（通过角色编码）
     */
    void bindUserRoleByCode(Long userId, String roleCode);

    /**
     * 查询用户的鉴权码列表（角色编码 + 权限编码）
     */
    List<String> listAuthoritiesByUserId(Long userId);

    /**
     * 查询角色列表（含角色权限编码）
     */
    List<RoleInfo> listRoles();

    /**
     * 查询权限列表
     */
    List<PermissionInfo> listPermissions();

    /**
     * 查询用户角色编码列表
     */
    List<String> listRoleCodesByUserId(Long userId);

    /**
     * 覆盖用户角色（通过角色编码）
     */
    void replaceUserRolesByCodes(Long userId, List<String> roleCodes);
}
