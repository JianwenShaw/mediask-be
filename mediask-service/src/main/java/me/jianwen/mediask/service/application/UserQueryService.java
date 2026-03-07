package me.jianwen.mediask.service.application;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.service.application.dto.user.UserDTO;
import me.jianwen.mediask.user.domain.entity.User;
import me.jianwen.mediask.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * 用户查询服务
 */
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    public UserDTO getCurrentUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        return UserDTO.builder()
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
