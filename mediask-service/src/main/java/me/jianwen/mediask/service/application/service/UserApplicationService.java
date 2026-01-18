package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.repository.UserRepository;
import me.jianwen.mediask.service.application.response.CurrentUserResponse;
import me.jianwen.mediask.user.domain.entity.User;
import org.springframework.stereotype.Service;

/**
 * 用户应用服务
 * 负责用户信息查询等业务用例
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;

    /**
     * 获取当前用户信息
     */
    public CurrentUserResponse getCurrentUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        return CurrentUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .userType(user.getUserType() != null ? user.getUserType().code() : null)
                .realName(user.getRealName())
                .gender(user.getGender() != null ? user.getGender().code() : null)
                .birthDate(user.getBirthDate())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
