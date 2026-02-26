package me.jianwen.mediask.api.advice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.api.security.SecurityAuditUtil;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.common.result.Result;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 业务异常 - 由 BizException 携带的错误码和消息直接返回给客户端
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        log.warn("业务异常: code={}, msg={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 系统异常 - 仅返回通用错误提示，详细信息记录在日志中，防止敏感信息泄露
     */
    @ExceptionHandler(SysException.class)
    public Result<Void> handleSysException(SysException ex) {
        log.error("系统异常: code={}, msg={}", ex.getCode(), ex.getMessage(), ex);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }

    /**
     * 参数校验异常 - 处理 @Valid/@Validated 校验失败和请求体解析失败，返回所有校验错误
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleValidation(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof BindException bind) {
            // MethodArgumentNotValidException extends BindException，统一处理
            String errors = bind.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + " " + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            if (!errors.isEmpty()) {
                message = errors;
            }
        } else if (ex instanceof HttpMessageNotReadableException) {
            message = "请求体解析失败";
        }
        log.warn("参数异常: {}", message);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 缺参异常 - 必要的请求参数未传递
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "缺少必要参数: " + ex.getParameterName();
        log.warn("缺参异常: {}", message);
        return Result.fail(ErrorCode.PARAM_MISSING.getCode(), message);
    }

    /**
     * 参数类型错误 - 请求参数无法转换为目标类型
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "参数类型错误: " + ex.getName();
        log.warn("类型异常: {}, expectedType={}", message,
                ex.getRequiredType() == null ? "unknown" : ex.getRequiredType().getSimpleName());
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 约束校验异常 - 处理 @Validated 注解在方法参数上的校验失败，返回所有校验错误
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        String message = (violations == null || violations.isEmpty())
                ? "参数校验失败"
                : violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .collect(Collectors.joining("; "));
        log.warn("约束异常: {}", message);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 非法参数异常兜底 - 返回固定消息防止内部信息泄露，以 error 级别记录堆栈便于排查
     * <p>
     * 注意：Domain 层值对象校验抛出的 IllegalArgumentException 应在 Service 层显式 catch 并转换为 BizException。
     * 此处仅作为未被 Service 层捕获的 IAE 的安全兜底。
     * </p>
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("未预期的IllegalArgumentException: {}", ex.getMessage(), ex);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), "参数不合法");
    }

    /**
     * 不支持的请求方法 - 如 GET 请求发到 POST 接口
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("不支持的请求方法: {}, supported={}", ex.getMethod(), ex.getSupportedMethods());
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "不支持的请求方法: " + ex.getMethod());
    }

    /**
     * 不支持的媒体类型 - Content-Type 不对
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("不支持的媒体类型: {}", ex.getContentType());
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), "不支持的媒体类型");
    }

    /**
     * 权限拒绝 - Spring Security 方法级权限检查失败
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public Result<Void> handleAccessDenied(Exception ex, HttpServletRequest request) {
        log.warn("方法权限拒绝(403): method={}, path={}, ip={}, userId={}, authorities={}, reason={}",
                request.getMethod(),
                SecurityAuditUtil.requestPath(request),
                SecurityAuditUtil.clientIp(request),
                SecurityAuditUtil.currentUserId(),
                SecurityAuditUtil.currentAuthorities(),
                ex.getMessage());
        return Result.fail(ErrorCode.ACCESS_DENIED);
    }

    /**
     * 兜底异常处理 - 捕获所有未被上述处理器匹配的异常，返回通用系统错误，防止敏感信息泄露
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
