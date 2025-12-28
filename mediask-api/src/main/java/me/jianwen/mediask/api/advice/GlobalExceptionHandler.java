package me.jianwen.mediask.api.advice;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.constant.ErrorCode;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.result.Result;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        log.warn("业务异常: code={}, msg={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, HttpMessageNotReadableException.class})
    public Result<Void> handleValidation(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException manv) {
            message = manv.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(err -> err.getField() + " " + err.getDefaultMessage())
                    .orElse(message);
        } else if (ex instanceof BindException bind) {
            message = bind.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(err -> err.getField() + " " + err.getDefaultMessage())
                    .orElse(message);
        } else if (ex instanceof HttpMessageNotReadableException) {
            message = "请求体解析失败";
        }
        log.warn("参数异常: {}", message);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}

