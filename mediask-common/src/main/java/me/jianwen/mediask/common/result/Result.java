package me.jianwen.mediask.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.jianwen.mediask.common.constant.ErrorCode;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应体
 * <p>
 * 所有 API 接口统一使用此类封装响应结果，保证前后端交互格式一致。
 * </p>
 *
 * @param <T> 响应数据类型
 * @author jianwen
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务码（0 表示成功，其他表示失败）
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 全链路追踪 ID（用于日志追踪）
     */
    private String traceId;

    /**
     * 响应时间戳
     */
    private Long timestamp;

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> ok(T data) {
        return Result.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .msg(ErrorCode.SUCCESS.getMsg())
                .data(data)
                .traceId(MDC.get("traceId"))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param msg  自定义消息
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Result<T> ok(String msg, T data) {
        return Result.<T>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .msg(msg)
                .data(data)
                .traceId(MDC.get("traceId"))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应（使用错误码）
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return Result.<T>builder()
                .code(errorCode.getCode())
                .msg(errorCode.getMsg())
                .traceId(MDC.get("traceId"))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应（自定义错误码和消息）
     *
     * @param code 错误码
     * @param msg  错误消息
     * @param <T>  数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> fail(Integer code, String msg) {
        return Result.<T>builder()
                .code(code)
                .msg(msg)
                .traceId(MDC.get("traceId"))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应（仅自定义消息，使用默认系统错误码）
     *
     * @param msg 错误消息
     * @param <T> 数据类型
     * @return 失败响应对象
     */
    public static <T> Result<T> fail(String msg) {
        return fail(ErrorCode.SYSTEM_ERROR.getCode(), msg);
    }

    /**
     * 判断是否成功
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return ErrorCode.SUCCESS.getCode().equals(this.code);
    }
}
