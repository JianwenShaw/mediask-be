package me.jianwen.mediask.common.exception;

import lombok.Getter;
import me.jianwen.mediask.common.enums.ErrorCode;

import java.io.Serial;

/**
 * 系统异常
 * <p>
 * 用于表示系统级错误，如数据库异常、缓存异常、外部服务调用失败等。
 * 系统异常会被全局异常处理器捕获，返回通用错误提示，并记录详细日志。
 * </p>
 *
 * @author jianwen
 */
@Getter
public class SysException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误码枚举
     */
    private final ErrorCode errorCode;

    /**
     * 使用错误码枚举构造
     *
     * @param errorCode 错误码枚举
     */
    public SysException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码枚举和自定义消息构造
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误消息
     */
    public SysException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用错误码枚举和异常原因构造
     *
     * @param errorCode 错误码枚举
     * @param cause     异常原因
     */
    public SysException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMsg(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用默认系统错误构造
     *
     * @param message 错误消息
     */
    public SysException(String message) {
        super(message);
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
        this.errorCode = ErrorCode.SYSTEM_ERROR;
    }

    /**
     * 使用默认系统错误和异常原因构造
     *
     * @param message 错误消息
     * @param cause   异常原因
     */
    public SysException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
        this.errorCode = ErrorCode.SYSTEM_ERROR;
    }
}
