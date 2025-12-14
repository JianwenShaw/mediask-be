package me.jianwen.mediask.common.lock.exception;

/**
 * 锁异常基类
 * <p>
 * 职责：统一分布式锁相关异常的基类
 * 设计原则：继承 RuntimeException，无需强制捕获，由全局异常处理器统一处理
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
public class LockException extends RuntimeException {

    /**
     * 错误码（预留扩展）
     */
    private String errorCode;

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LockException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
