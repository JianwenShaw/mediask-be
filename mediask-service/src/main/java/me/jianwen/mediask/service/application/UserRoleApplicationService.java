package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.util.AssertUtil;
import me.jianwen.mediask.service.application.command.UpdateUserRolesCommand;
import me.jianwen.mediask.user.domain.repository.AuthzRepository;
import me.jianwen.mediask.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户角色应用服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserRoleApplicationService {

    private final AuthzRepository authzRepository;
    private final UserRepository userRepository;

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
