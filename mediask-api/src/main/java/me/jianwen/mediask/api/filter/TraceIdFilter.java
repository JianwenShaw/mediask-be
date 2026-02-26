package me.jianwen.mediask.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.jianwen.mediask.common.constant.CommonConstants;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * TraceId 过滤器
 * <p>
 * 从请求头 {@code X-Trace-Id} 读取或自动生成 traceId，写入 MDC 用于日志链路追踪，
 * 同时回写到响应头。请求完成后清理 MDC 上下文。
 * </p>
 * <p>
 * 对外部传入的 traceId 进行安全校验（长度限制 + 字符白名单），防止日志注入攻击。
 * </p>
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final int MAX_TRACE_ID_LENGTH = 64;
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)
                || traceId.length() > MAX_TRACE_ID_LENGTH
                || !TRACE_ID_PATTERN.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(CommonConstants.MDC_TRACE_ID, traceId);
        MDC.put(CommonConstants.MDC_REQUEST_URI, request.getRequestURI());
        response.setHeader(CommonConstants.HEADER_TRACE_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CommonConstants.MDC_TRACE_ID);
            MDC.remove(CommonConstants.MDC_REQUEST_URI);
        }
    }
}
