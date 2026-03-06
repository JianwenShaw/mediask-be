package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.util.AssertUtil;
import me.jianwen.mediask.service.application.command.UpdateUserRolesCommand;
import me.jianwen.mediask.service.application.dto.auth.PermissionDTO;
import me.jianwen.mediask.service.application.dto.auth.RoleDTO;
import me.jianwen.mediask.service.application.dto.auth.UserRoleDTO;
import me.jianwen.mediask.user.domain.entity.PermissionInfo;
import me.jianwen.mediask.user.domain.entity.RoleInfo;
import me.jianwen.mediask.user.domain.repository.AuthzRepository;
import me.jianwen.mediask.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthzApplicationService {

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

    @Transactional(rollbackFor = Exception.class)
    public void updateUserRoles(Long userId, UpdateUserRolesCommand command) {
        validateUserExists(userId);
        AssertUtil.notNull(command, ErrorCode.PARAM_MISSING);
        AssertUtil.notEmpty(command.getRoleCodes(), "角色编码不能为空");
        authzRepository.replaceUserRolesByCodes(userId, command.getRoleCodes());
        log.info("更新用户角色成功: userId={}, roleCodes={}", userId, command.getRoleCodes());
    }

    private void validateUserExists(Long userId) {
        AssertUtil.notNull(userId, ErrorCode.PARAM_MISSING);
        userRepository.findById(userId).orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }
}
