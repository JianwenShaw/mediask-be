package me.jianwen.mediask.domain.repository;

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
}
