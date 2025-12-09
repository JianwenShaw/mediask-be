package me.jianwen.mediask.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 统一错误码枚举
 * <p>
 * 错误码规范：
 * <ul>
 *   <li>0: 成功</li>
 *   <li>1xxx: 通用错误（参数、权限等）</li>
 *   <li>2xxx: 用户模块错误</li>
 *   <li>3xxx: 挂号预约模块错误</li>
 *   <li>4xxx: 医生/排班模块错误</li>
 *   <li>5xxx: 病历模块错误</li>
 *   <li>6xxx: AI 问诊模块错误</li>
 *   <li>9xxx: 系统级错误</li>
 * </ul>
 * </p>
 *
 * @author jianwen
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS(0, "success"),

    // ==================== 通用错误 (1xxx) ====================
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "缺少必要参数"),
    PARAM_INVALID(1003, "参数格式不正确"),
    DATA_NOT_FOUND(1004, "数据不存在"),
    DATA_DUPLICATE(1005, "数据已存在"),
    OPERATION_FORBIDDEN(1006, "操作被禁止"),
    RATE_LIMIT_EXCEEDED(1007, "请求过于频繁，请稍后再试"),
    IDEMPOTENT_REQUEST(1008, "重复请求，请勿重复提交"),

    // ==================== 认证授权错误 (11xx) ====================
    UNAUTHORIZED(1100, "未登录或登录已过期"),
    TOKEN_INVALID(1101, "Token 无效"),
    TOKEN_EXPIRED(1102, "Token 已过期"),
    ACCESS_DENIED(1103, "权限不足"),
    ACCOUNT_DISABLED(1104, "账号已被禁用"),
    ACCOUNT_LOCKED(1105, "账号已被锁定"),

    // ==================== 用户模块错误 (2xxx) ====================
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_PASSWORD_ERROR(2002, "用户名或密码错误"),
    USER_PHONE_EXISTS(2003, "手机号已被注册"),
    USER_EMAIL_EXISTS(2004, "邮箱已被注册"),
    USER_ID_CARD_EXISTS(2005, "身份证号已被注册"),
    USER_OLD_PASSWORD_ERROR(2006, "原密码错误"),
    USER_REGISTER_FAILED(2007, "用户注册失败"),
    VERIFY_CODE_ERROR(2008, "验证码错误"),
    VERIFY_CODE_EXPIRED(2009, "验证码已过期"),
    VERIFY_CODE_SEND_FAILED(2010, "验证码发送失败"),

    // ==================== 挂号预约模块错误 (3xxx) ====================
    APPT_NO_SLOTS(3001, "该时段号源已挂完"),
    APPT_TIME_CONFLICT(3002, "该时间段您已有挂号记录"),
    APPT_CANCEL_FAILED(3003, "取消挂号失败"),
    APPT_CANCEL_TIMEOUT(3004, "已超过取消挂号时限"),
    APPT_NOT_FOUND(3005, "挂号记录不存在"),
    APPT_STATUS_ERROR(3006, "挂号状态异常"),
    APPT_PAY_TIMEOUT(3007, "支付已超时，请重新挂号"),
    APPT_BUSY(3008, "系统繁忙，请稍后重试"),
    APPT_LOCKED(3009, "号源正在被锁定，请稍后重试"),
    SCHEDULE_NOT_FOUND(3010, "排班信息不存在"),
    SCHEDULE_UNAVAILABLE(3011, "该排班不可预约"),

    // ==================== 医生模块错误 (4xxx) ====================
    DOCTOR_NOT_FOUND(4001, "医生信息不存在"),
    DOCTOR_NOT_AVAILABLE(4002, "该医生暂不接诊"),
    DEPARTMENT_NOT_FOUND(4003, "科室信息不存在"),
    HOSPITAL_NOT_FOUND(4004, "医院信息不存在"),

    // ==================== 病历模块错误 (5xxx) ====================
    EMR_NOT_FOUND(5001, "病历记录不存在"),
    EMR_ACCESS_DENIED(5002, "无权访问该病历"),
    EMR_CREATE_FAILED(5003, "病历创建失败"),
    EMR_UPDATE_FAILED(5004, "病历更新失败"),

    // ==================== AI 问诊模块错误 (6xxx) ====================
    AI_SERVICE_UNAVAILABLE(6001, "AI 服务暂不可用"),
    AI_REQUEST_TIMEOUT(6002, "AI 响应超时"),
    AI_RESPONSE_ERROR(6003, "AI 响应异常"),
    AI_KNOWLEDGE_NOT_FOUND(6004, "未找到相关医学知识"),
    AI_CONTENT_UNSAFE(6005, "问诊内容包含敏感信息"),

    // ==================== 文件存储模块错误 (7xxx) ====================
    FILE_UPLOAD_FAILED(7001, "文件上传失败"),
    FILE_NOT_FOUND(7002, "文件不存在"),
    FILE_TYPE_NOT_ALLOWED(7003, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(7004, "文件大小超过限制"),

    // ==================== 系统级错误 (9xxx) ====================
    SYSTEM_ERROR(9999, "系统繁忙，请稍后再试"),
    DATABASE_ERROR(9001, "数据库操作异常"),
    REDIS_ERROR(9002, "缓存服务异常"),
    MQ_ERROR(9003, "消息队列异常"),
    RPC_ERROR(9004, "远程调用失败"),
    THIRD_PARTY_ERROR(9005, "第三方服务异常");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String msg;

    /**
     * 根据错误码获取枚举
     *
     * @param code 错误码
     * @return ErrorCode 枚举，不存在则返回 SYSTEM_ERROR
     */
    public static ErrorCode fromCode(Integer code) {
        if (code == null) {
            return SYSTEM_ERROR;
        }
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElse(SYSTEM_ERROR);
    }
}
