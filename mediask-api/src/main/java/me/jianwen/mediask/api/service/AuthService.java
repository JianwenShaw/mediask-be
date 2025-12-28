package me.jianwen.mediask.api.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.api.model.auth.LoginRequest;
import me.jianwen.mediask.api.model.auth.LoginResponse;
import me.jianwen.mediask.api.model.auth.RegisterRequest;
import me.jianwen.mediask.api.security.JwtService;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.util.AssertUtil;
import me.jianwen.mediask.dal.entity.UserDO;
import me.jianwen.mediask.dal.enums.GenderEnum;
import me.jianwen.mediask.dal.enums.UserTypeEnum;
import me.jianwen.mediask.dal.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证相关应用服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        AssertUtil.notNull(request, ErrorCode.PARAM_MISSING);

        // 唯一性校验
        long usernameExists = userMapper.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, request.getUsername()));
        if (usernameExists > 0) {
            throw new BizException(ErrorCode.USER_REGISTER_FAILED, "用户名已存在");
        }

        if (StringUtils.hasText(request.getPhone())) {
            long phoneExists = userMapper.selectCount(new LambdaQueryWrapper<UserDO>()
                    .eq(UserDO::getPhone, request.getPhone()));
            if (phoneExists > 0) {
                throw new BizException(ErrorCode.USER_PHONE_EXISTS);
            }
        }

        UserTypeEnum userType = resolveUserType(request.getUserType());

        UserDO user = new UserDO();
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserType(userType);
        user.setRealName(request.getRealName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setBirthDate(request.getBirthDate());
        if (request.getGender() != null) {
            user.setGender(resolveGender(request.getGender()));
        }

        int inserted = userMapper.insert(user);
        if (inserted != 1) {
            throw new BizException(ErrorCode.USER_REGISTER_FAILED);
        }

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        AssertUtil.notNull(request, ErrorCode.PARAM_MISSING);

        UserDO user = userMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, request.getAccount())
                .or(wrapper -> wrapper.eq(UserDO::getPhone, request.getAccount())));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }

        var authorities = deriveAuthorities(user.getUserType());
        JwtService.JwtToken token = jwtService.generateToken(user.getId(), user.getUsername(), user.getUserType(),
                authorities);

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType() != null ? user.getUserType().getCode() : null)
                .authorities(authorities)
                .token(token.token())
                .expireAt(token.expireAt())
                .build();
    }

    private UserTypeEnum resolveUserType(Integer code) {
        try {
            return UserTypeEnum.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法的用户类型");
        }
    }

    private GenderEnum resolveGender(Integer code) {
        try {
            return GenderEnum.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法的性别枚举");
        }
    }

    /**
     * 简单的权限派发：基于用户类型，预置一些接口权限
     * 后续可替换为基于角色/权限表的查询
     */
    private java.util.List<String> deriveAuthorities(UserTypeEnum userType) {
        if (userType == null) {
            return java.util.Collections.emptyList();
        }
        return switch (userType) {
            case ADMIN -> java.util.List.of(
                    "schedule:create", "schedule:auto", "schedule:update");
            case DOCTOR -> java.util.List.of("schedule:update");
            case PATIENT -> java.util.Collections.emptyList();
        };
    }
}
