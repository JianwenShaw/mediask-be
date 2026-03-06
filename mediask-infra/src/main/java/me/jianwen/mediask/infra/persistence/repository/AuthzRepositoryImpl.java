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
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.user.domain.entity.PermissionInfo;
import me.jianwen.mediask.user.domain.entity.RoleInfo;
import me.jianwen.mediask.user.domain.repository.AuthzRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Override
    public List<RoleInfo> listRoles() {
        List<RoleDO> roles = roleMapper.selectList(new LambdaQueryWrapper<RoleDO>()
                .orderByAsc(RoleDO::getRoleCode));
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = roles.stream().map(RoleDO::getId).toList();
        List<RolePermissionDO> rolePermissions = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermissionDO>()
                .in(RolePermissionDO::getRoleId, roleIds));

        Set<Long> permissionIds = rolePermissions.stream()
                .map(RolePermissionDO::getPermissionId)
                .collect(Collectors.toSet());
        Map<Long, PermissionDO> permissionMap = permissionIds.isEmpty()
                ? Map.of()
                : permissionMapper.selectBatchIds(permissionIds).stream()
                        .collect(Collectors.toMap(PermissionDO::getId, p -> p));

        Map<Long, List<String>> rolePermissionCodeMap = new HashMap<>();
        for (RolePermissionDO rolePermission : rolePermissions) {
            PermissionDO permission = permissionMap.get(rolePermission.getPermissionId());
            if (permission == null || !StringUtils.hasText(permission.getPermCode())) {
                continue;
            }
            rolePermissionCodeMap.computeIfAbsent(rolePermission.getRoleId(), k -> new ArrayList<>())
                    .add(permission.getPermCode());
        }

        return roles.stream()
                .map(role -> {
                    List<String> permissionCodes = rolePermissionCodeMap.getOrDefault(role.getId(), List.of()).stream()
                            .distinct()
                            .sorted()
                            .toList();
                    return new RoleInfo(
                            role.getId(),
                            role.getRoleCode(),
                            role.getRoleName(),
                            role.getDescription(),
                            permissionCodes);
                })
                .toList();
    }

    @Override
    public List<PermissionInfo> listPermissions() {
        List<PermissionDO> permissions = permissionMapper.selectList(new LambdaQueryWrapper<PermissionDO>()
                .orderByAsc(PermissionDO::getPermCode));
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        return permissions.stream()
                .map(permission -> new PermissionInfo(
                        permission.getId(),
                        permission.getPermCode(),
                        permission.getPermName(),
                        permission.getDescription()))
                .toList();
    }

    @Override
    public List<String> listRoleCodesByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<UserRoleDO> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getUserId, userId));
        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream().map(UserRoleDO::getRoleId).toList();
        List<RoleDO> roles = roleMapper.selectBatchIds(roleIds);
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .map(RoleDO::getRoleCode)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public void replaceUserRolesByCodes(Long userId, List<String> roleCodes) {
        if (userId == null) {
            return;
        }
        List<String> normalizedCodes = roleCodes == null ? List.of() : roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_MISSING, "角色编码不能为空");
        }

        List<RoleDO> roles = normalizedCodes.stream()
                .map(this::findRoleByCode)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(RoleDO::getId))
                .toList();
        if (roles.size() != normalizedCodes.size()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "存在无效角色编码");
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, userId));

        for (RoleDO role : roles) {
            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRoleMapper.insert(userRole);
        }
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
