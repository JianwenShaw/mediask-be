package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.util.AssertUtil;
import me.jianwen.mediask.domain.ratelimit.RateLimiterService;
import me.jianwen.mediask.domain.repository.AuthzRepository;
import me.jianwen.mediask.domain.repository.UserRepository;
import me.jianwen.mediask.infra.cache.RateLimitKeyManager;
import me.jianwen.mediask.infra.security.JwtService;
import me.jianwen.mediask.infra.security.RefreshTokenStore;
import me.jianwen.mediask.common.dto.auth.LoginDTO;
import me.jianwen.mediask.service.application.command.LoginCommand;
import me.jianwen.mediask.service.application.command.RegisterCommand;
import me.jianwen.mediask.user.domain.entity.User;
import me.jianwen.mediask.user.domain.enums.Gender;
import me.jianwen.mediask.user.domain.enums.UserType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * 认证应用服务
 * 负责用户注册、登录、Token刷新等业务用例
 *
 * @author jianwen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final AuthzRepository authzRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final RateLimiterService rateLimiterService;

    private static final long SINGLE_ACQUIRE_PERMITS = 1L;
    private static final long DEFAULT_LOGIN_ACCOUNT_LIMIT = 10L;
    private static final long DEFAULT_LOGIN_ACCOUNT_WINDOW_SECONDS = 60L;
    private static final long DEFAULT_LOGIN_IP_LIMIT = 30L;
    private static final long DEFAULT_LOGIN_IP_WINDOW_SECONDS = 60L;

    @Value("${mediask.rate-limit.auth-login.account.permits:" + DEFAULT_LOGIN_ACCOUNT_LIMIT + "}")
    private long loginAccountRateLimitPermits;

    @Value("${mediask.rate-limit.auth-login.account.window-seconds:" + DEFAULT_LOGIN_ACCOUNT_WINDOW_SECONDS + "}")
    private long loginAccountRateLimitWindowSeconds;

    @Value("${mediask.rate-limit.auth-login.ip.permits:" + DEFAULT_LOGIN_IP_LIMIT + "}")
    private long loginIpRateLimitPermits;

    @Value("${mediask.rate-limit.auth-login.ip.window-seconds:" + DEFAULT_LOGIN_IP_WINDOW_SECONDS + "}")
    private long loginIpRateLimitWindowSeconds;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginDTO register(RegisterCommand request) {
        AssertUtil.notNull(request, ErrorCode.PARAM_MISSING);

        // 唯一性校验
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BizException(ErrorCode.USER_REGISTER_FAILED, "用户名已存在");
        }

        if (StringUtils.hasText(request.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BizException(ErrorCode.USER_PHONE_EXISTS);
            }
        }

        UserType userType = resolveUserType(request.getUserType());
        Gender gender = request.getGender() != null ? resolveGender(request.getGender()) : null;

        User user = User.builder()
                .username(request.getUsername())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(userType)
                .realName(request.getRealName())
                .avatarUrl(request.getAvatarUrl())
                .birthDate(request.getBirthDate())
                .gender(gender)
                .build();

        Long userId = userRepository.insert(user);
        if (userId == null) {
            throw new BizException(ErrorCode.USER_REGISTER_FAILED);
        }

        // 绑定默认角色
        authzRepository.bindUserRoleByCode(userId, resolveDefaultRoleCode(request.getUserType()));

        // 注册成功后自动生成 Token
        var authorities = authzRepository.listAuthoritiesByUserId(userId);
        Integer userTypeCode = userType != null ? userType.code() : null;
        JwtService.JwtToken access = jwtService.generateAccessToken(userId, request.getUsername(), userTypeCode, authorities);
        JwtService.JwtToken refresh = jwtService.generateRefreshToken(userId, request.getUsername(), userTypeCode, authorities);

        // 存储 Refresh Token 到 Redis
        refreshTokenStore.store(userId, refresh.tokenId(), jwtService.getRefreshExpireSeconds());

        long nowSec = Instant.now().getEpochSecond();
        log.info("用户注册成功: userId={}, username={}", userId, request.getUsername());
        return LoginDTO.builder()
                .userId(userId)
                .username(request.getUsername())
                .userType(userTypeCode)
                .authorities(authorities)
                .tokenType("Bearer")
                .token(access.token())
                .expireAt(access.expireAt())
                .expiresIn(Math.max(0, access.expireAt() - nowSec))
                .refreshToken(refresh.token())
                .refreshTokenId(refresh.tokenId())
                .build();
    }

    /**
     * 用户登录
     */
    public LoginDTO login(LoginCommand request) {
        AssertUtil.notNull(request, ErrorCode.PARAM_MISSING);
        assertLoginRateLimit(request.getAccount(), request.getClientIp());

        User user = userRepository.findByUsernameOrPhone(request.getAccount()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.USER_PASSWORD_ERROR);
        }

        var authorities = authzRepository.listAuthoritiesByUserId(user.getId());
        Integer userTypeCode = user.getUserType() != null ? user.getUserType().code() : null;
        JwtService.JwtToken access = jwtService.generateAccessToken(user.getId(), user.getUsername(), userTypeCode, authorities);
        JwtService.JwtToken refresh = jwtService.generateRefreshToken(user.getId(), user.getUsername(), userTypeCode, authorities);

        // 存储 Refresh Token 到 Redis
        refreshTokenStore.store(user.getId(), refresh.tokenId(), jwtService.getRefreshExpireSeconds());

        long nowSec = Instant.now().getEpochSecond();
        return LoginDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .userType(user.getUserType() != null ? user.getUserType().code() : null)
                .authorities(authorities)
                .tokenType("Bearer")
                .token(access.token())
                .expireAt(access.expireAt())
                .expiresIn(Math.max(0, access.expireAt() - nowSec))
                .refreshToken(refresh.token())
                .refreshTokenId(refresh.tokenId())
                .build();
    }

    private void assertLoginRateLimit(String account, String clientIp) {
        String ipRateLimitKey = RateLimitKeyManager.authLoginIpRateLimitKey(clientIp);
        boolean ipAllowed = rateLimiterService.tryAcquire(
                ipRateLimitKey,
                SINGLE_ACQUIRE_PERMITS,
                resolveLoginIpRateLimitPermits(),
                Duration.ofSeconds(resolveLoginIpRateLimitWindowSeconds())
        );
        if (!ipAllowed) {
            throw new BizException(ErrorCode.RATE_LIMIT_EXCEEDED, "该来源登录请求过于频繁，请稍后再试");
        }

        String accountRateLimitKey = RateLimitKeyManager.authLoginAccountRateLimitKey(account);
        boolean accountAllowed = rateLimiterService.tryAcquire(
                accountRateLimitKey,
                SINGLE_ACQUIRE_PERMITS,
                resolveLoginAccountRateLimitPermits(),
                Duration.ofSeconds(resolveLoginAccountRateLimitWindowSeconds())
        );
        if (!accountAllowed) {
            throw new BizException(ErrorCode.RATE_LIMIT_EXCEEDED, "该账号登录过于频繁，请稍后再试");
        }
    }

    private long resolveLoginAccountRateLimitPermits() {
        return loginAccountRateLimitPermits > 0 ? loginAccountRateLimitPermits : DEFAULT_LOGIN_ACCOUNT_LIMIT;
    }

    private long resolveLoginAccountRateLimitWindowSeconds() {
        return loginAccountRateLimitWindowSeconds > 0 ? loginAccountRateLimitWindowSeconds : DEFAULT_LOGIN_ACCOUNT_WINDOW_SECONDS;
    }

    private long resolveLoginIpRateLimitPermits() {
        return loginIpRateLimitPermits > 0 ? loginIpRateLimitPermits : DEFAULT_LOGIN_IP_LIMIT;
    }

    private long resolveLoginIpRateLimitWindowSeconds() {
        return loginIpRateLimitWindowSeconds > 0 ? loginIpRateLimitWindowSeconds : DEFAULT_LOGIN_IP_WINDOW_SECONDS;
    }

    /**
     * 刷新 token：使用 refreshToken 换取新的 access token（并轮换 refresh token）
     */
    public LoginDTO refresh(String refreshToken) {
        AssertUtil.notBlank(refreshToken, ErrorCode.PARAM_MISSING);

        JwtService.JwtPayload payload;
        try {
            payload = jwtService.parseToken(refreshToken);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        } catch (io.jsonwebtoken.JwtException ex) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        if (payload.tokenKind() != JwtService.TokenKind.REFRESH) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "refreshToken 类型不正确");
        }
        if (payload.userId() == null || !StringUtils.hasText(payload.username())) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "refreshToken 载荷不完整");
        }

        // 检查 Refresh Token 是否在 Redis 中有效
        if (payload.tokenId() == null || !refreshTokenStore.isValid(payload.userId(), payload.tokenId())) {
            throw new BizException(ErrorCode.TOKEN_INVALID, "refreshToken 已失效");
        }

        var authorities = authzRepository.listAuthoritiesByUserId(payload.userId());

        // 轮换 Refresh Token：新 Token 覆盖写，旧 Token 自动失效
        JwtService.JwtToken newAccess = jwtService.generateAccessToken(payload.userId(), payload.username(), payload.userType(), authorities);
        JwtService.JwtToken newRefresh = jwtService.generateRefreshToken(payload.userId(), payload.username(), payload.userType(), authorities);
        refreshTokenStore.store(payload.userId(), newRefresh.tokenId(), jwtService.getRefreshExpireSeconds());

        long nowSec = Instant.now().getEpochSecond();
        return LoginDTO.builder()
                .userId(payload.userId())
                .username(payload.username())
                .userType(payload.userType())
                .authorities(authorities)
                .tokenType("Bearer")
                .token(newAccess.token())
                .expireAt(newAccess.expireAt())
                .expiresIn(Math.max(0, newAccess.expireAt() - nowSec))
                .refreshToken(newRefresh.token())
                .refreshTokenId(newRefresh.tokenId())
                .build();
    }

    /**
     * 登出：撤销当前用户的 Refresh Token
     *
     * @param userId 用户ID
     */
    public void logout(Long userId) {
        refreshTokenStore.remove(userId);
        log.info("用户登出: userId={}", userId);
    }

    private UserType resolveUserType(Integer code) {
        try {
            return UserType.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法的用户类型");
        }
    }

    private Gender resolveGender(Integer code) {
        try {
            return Gender.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, "非法的性别枚举");
        }
    }

    /**
     * 用户类型约定：1-患者 2-医生 3-管理员
     */
    private String resolveDefaultRoleCode(Integer userTypeCode) {
        if (userTypeCode == null) {
            return "patient";
        }
        return switch (userTypeCode) {
            case 1 -> "patient";
            case 2 -> "doctor";
            case 3 -> "admin";
            default -> "patient";
        };
    }
}
