package me.jianwen.mediask.common.exception;

import lombok.Getter;
import me.jianwen.mediask.common.enums.ErrorCode;

import java.io.Serial;

/**
 * 业务异常
 * <p>
 * 用于表示业务逻辑错误，如参数校验失败、业务规则不满足等。
 * 业务异常会被全局异常处理器捕获并返回对应的错误码和消息。
 * </p>
 *
 * @author jianwen
 */
@Getter
public class BizException extends RuntimeException {

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
    public BizException(ErrorCode errorCode) {
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
    public BizException(ErrorCode errorCode, String message) {
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
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMsg(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义错误码和消息构造
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorCode = ErrorCode.fromCode(code);
    }
}
