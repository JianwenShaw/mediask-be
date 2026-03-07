package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.service.application.dto.auth.PermissionDTO;
import me.jianwen.mediask.service.application.dto.auth.RoleDTO;
import me.jianwen.mediask.service.application.dto.auth.UserRoleDTO;
import me.jianwen.mediask.user.domain.entity.PermissionInfo;
import me.jianwen.mediask.user.domain.entity.RoleInfo;
import me.jianwen.mediask.user.domain.repository.AuthzRepository;
import me.jianwen.mediask.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限查询服务
 */
@Service
@RequiredArgsConstructor
public class AuthorizationQueryService {

    private final AuthzRepository authzRepository;
    private final UserRepository userRepository;

    public List<RoleDTO> listRoles() {
        List<RoleInfo> roles = authzRepository.listRoles();
        return roles.stream()
                .map(role -> RoleDTO.builder()
                        .roleId(role.roleId())
                        .roleCode(role.roleCode())
                        .roleName(role.roleName())
                        .description(role.description())
                        .permissionCodes(role.permissionCodes())
                        .build())
                .toList();
    }

    public List<PermissionDTO> listPermissions() {
        List<PermissionInfo> permissions = authzRepository.listPermissions();
        return permissions.stream()
                .map(permission -> PermissionDTO.builder()
                        .permissionId(permission.permissionId())
                        .permissionCode(permission.permissionCode())
                        .permissionName(permission.permissionName())
                        .description(permission.description())
                        .build())
                .toList();
    }

    public UserRoleDTO getUserRoles(Long userId) {
        validateUserExists(userId);
        return UserRoleDTO.builder()
                .userId(userId)
                .roleCodes(authzRepository.listRoleCodesByUserId(userId))
                .build();
    }

    private void validateUserExists(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.PARAM_MISSING);
        }
        userRepository.findById(userId).orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }
}
