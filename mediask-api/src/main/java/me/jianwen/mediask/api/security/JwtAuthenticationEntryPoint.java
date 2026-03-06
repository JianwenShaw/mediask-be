package me.jianwen.mediask.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.api.util.SecurityAuditUtil;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.common.util.JsonUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 未认证处理
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        boolean hasAuthorizationHeader = StringUtils.hasText(request.getHeader("Authorization"));
        log.warn("未认证访问(401): method={}, path={}, ip={}, hasAuthorizationHeader={}, reason={}",
                request.getMethod(),
                SecurityAuditUtil.requestPath(request),
                SecurityAuditUtil.clientIp(request),
                hasAuthorizationHeader,
                authException.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.fail(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(JsonUtil.toJson(body));
    }
}
