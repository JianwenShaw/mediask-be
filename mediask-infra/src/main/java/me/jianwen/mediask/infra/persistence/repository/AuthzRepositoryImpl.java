package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.dal.entity.PermissionDO;
import me.jianwen.mediask.dal.entity.RoleDO;
import me.jianwen.mediask.dal.entity.RolePermissionDO;
import me.jianwen.mediask.dal.entity.UserRoleDO;
import me.jianwen.mediask.dal.mapper.PermissionMapper;
import me.jianwen.mediask.dal.mapper.RoleMapper;
import me.jianwen.mediask.dal.mapper.RolePermissionMapper;
import me.jianwen.mediask.dal.mapper.UserRoleMapper;
import me.jianwen.mediask.domain.repository.AuthzRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AuthzRepositoryImpl implements AuthzRepository {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public void bindUserRoleByCode(Long userId, String roleCode) {
        if (userId == null || !StringUtils.hasText(roleCode)) {
            return;
        }

        RoleDO role = findRoleByCode(roleCode);
        if (role == null) {
            return;
        }

        LambdaQueryWrapper<UserRoleDO> existsWrapper = new LambdaQueryWrapper<>();
        existsWrapper.eq(UserRoleDO::getUserId, userId)
                .eq(UserRoleDO::getRoleId, role.getId());
        if (userRoleMapper.selectCount(existsWrapper) > 0) {
            return;
        }

        UserRoleDO userRole = new UserRoleDO();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
    }

    @Override
    public List<String> listAuthoritiesByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }

        LambdaQueryWrapper<UserRoleDO> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(UserRoleDO::getUserId, userId);
        List<UserRoleDO> userRoles = userRoleMapper.selectList(userRoleWrapper);
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoles.stream().map(UserRoleDO::getRoleId).toList();
        List<RoleDO> roles = roleMapper.selectBatchIds(roleIds);

        Set<String> authorities = new LinkedHashSet<>();
        roles.stream()
                .map(RoleDO::getRoleCode)
                .filter(StringUtils::hasText)
                .forEach(code -> {
                    authorities.add(code);
                    authorities.add(code.toLowerCase());
                });

        LambdaQueryWrapper<RolePermissionDO> rpWrapper = new LambdaQueryWrapper<>();
        rpWrapper.in(RolePermissionDO::getRoleId, roleIds);
        List<RolePermissionDO> rolePermissions = rolePermissionMapper.selectList(rpWrapper);
        if (rolePermissions == null || rolePermissions.isEmpty()) {
            return new ArrayList<>(authorities);
        }

        List<Long> permissionIds = rolePermissions.stream()
                .map(RolePermissionDO::getPermissionId)
                .distinct()
                .collect(Collectors.toList());
        List<PermissionDO> permissions = permissionMapper.selectBatchIds(permissionIds);
        permissions.stream()
                .map(PermissionDO::getPermCode)
                .filter(StringUtils::hasText)
                .forEach(authorities::add);

        return new ArrayList<>(authorities);
    }

    private RoleDO findRoleByCode(String roleCode) {
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDO::getRoleCode, roleCode);
        RoleDO role = roleMapper.selectOne(wrapper);
        if (role != null) {
            return role;
        }

        LambdaQueryWrapper<RoleDO> upperWrapper = new LambdaQueryWrapper<>();
        upperWrapper.eq(RoleDO::getRoleCode, roleCode.toUpperCase());
        role = roleMapper.selectOne(upperWrapper);
        if (role != null) {
            return role;
        }

        LambdaQueryWrapper<RoleDO> lowerWrapper = new LambdaQueryWrapper<>();
        lowerWrapper.eq(RoleDO::getRoleCode, roleCode.toLowerCase());
        return roleMapper.selectOne(lowerWrapper);
    }
}
