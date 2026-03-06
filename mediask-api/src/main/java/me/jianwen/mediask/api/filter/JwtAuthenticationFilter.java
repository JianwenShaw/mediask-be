package me.jianwen.mediask.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.api.util.SecurityAuditUtil;
import me.jianwen.mediask.service.application.dto.auth.AccessTokenPrincipalDTO;
import me.jianwen.mediask.service.application.TokenApplicationService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenApplicationService tokenApplicationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            AccessTokenPrincipalDTO principal = tokenApplicationService.parseAccessToken(token).orElse(null);
            if (principal == null) {
                // refresh token 不允许作为 API 访问凭证
                log.warn("JWT 解析为空，拒绝建立认证上下文: method={}, path={}, ip={}",
                        request.getMethod(),
                        SecurityAuditUtil.requestPath(request),
                        SecurityAuditUtil.clientIp(request));
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            List<SimpleGrantedAuthority> authorities = principal.getAuthorities().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal.getUserId(), null, authorities);
            authentication.setDetails(principal);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            log.warn("JWT 校验失败: method={}, path={}, ip={}, reason={}",
                    request.getMethod(),
                    SecurityAuditUtil.requestPath(request),
                    SecurityAuditUtil.clientIp(request),
                    ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
