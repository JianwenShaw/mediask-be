package me.jianwen.mediask.user.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.dal.entity.UserDO;
import me.jianwen.mediask.dal.mapper.UserMapper;
import me.jianwen.mediask.user.application.dto.CurrentUserDTO;
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

    private final UserMapper userMapper;

    /**
     * 获取当前用户信息
     */
    public CurrentUserDTO getCurrentUser(Long userId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        return CurrentUserDTO.builder()
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

