package me.jianwen.mediask.api.service;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.api.model.user.CurrentUserResponse;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.dal.entity.UserDO;
import me.jianwen.mediask.dal.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户查询服务（API 层薄服务）
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public CurrentUserResponse getCurrentUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        return CurrentUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .userType(user.getUserType() != null ? user.getUserType().getCode() : null)
                .realName(user.getRealName())
                .gender(user.getGender() != null ? user.getGender().getCode() : null)
                .birthDate(user.getBirthDate())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}


