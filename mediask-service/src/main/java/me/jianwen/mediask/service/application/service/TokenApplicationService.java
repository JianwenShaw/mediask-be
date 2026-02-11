package me.jianwen.mediask.service.application.service;

import lombok.RequiredArgsConstructor;
import me.jianwen.mediask.common.dto.auth.AccessTokenPrincipalDTO;
import me.jianwen.mediask.infra.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Token 应用服务
 */
@Service
@RequiredArgsConstructor
public class TokenApplicationService {

    private final JwtService jwtService;

    public Optional<AccessTokenPrincipalDTO> parseAccessToken(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        try {
            JwtService.JwtPayload payload = jwtService.parseToken(token);
            if (payload.tokenKind() != JwtService.TokenKind.ACCESS || payload.userId() == null) {
                return Optional.empty();
            }
            List<String> authorities = payload.authorities() != null ? payload.authorities() : List.of();
            return Optional.of(AccessTokenPrincipalDTO.builder()
                    .userId(payload.userId())
                    .authorities(authorities)
                    .build());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
