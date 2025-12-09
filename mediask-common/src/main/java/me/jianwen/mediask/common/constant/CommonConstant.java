package me.jianwen.mediask.common.constant;

/**
 * 公共常量定义
 *
 * @author jianwen
 */
public interface CommonConstant {

    // ==================== 系统常量 ====================

    /**
     * 系统名称
     */
    String SYSTEM_NAME = "MediAsk";

    /**
     * 默认分页大小
     */
    int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大分页大小
     */
    int MAX_PAGE_SIZE = 100;

    /**
     * 默认起始页码
     */
    int DEFAULT_PAGE_NUM = 1;

    // ==================== 请求头常量 ====================

    /**
     * Token 请求头
     */
    String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Token 前缀
     */
    String TOKEN_PREFIX = "Bearer ";

    /**
     * 追踪 ID 请求头
     */
    String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 请求 ID 请求头
     */
    String HEADER_REQUEST_ID = "X-Request-Id";

    /**
     * 幂等性 Key 请求头
     */
    String HEADER_IDEMPOTENT_KEY = "X-Idempotent-Key";

    /**
     * 客户端类型请求头
     */
    String HEADER_CLIENT_TYPE = "X-Client-Type";

    // ==================== MDC 日志上下文 ====================

    /**
     * 追踪 ID（MDC）
     */
    String MDC_TRACE_ID = "traceId";

    /**
     * 用户 ID（MDC）
     */
    String MDC_USER_ID = "userId";

    /**
     * 请求 URI（MDC）
     */
    String MDC_REQUEST_URI = "requestUri";

    // ==================== 状态常量 ====================

    /**
     * 正常状态
     */
    int STATUS_NORMAL = 1;

    /**
     * 禁用状态
     */
    int STATUS_DISABLED = 0;

    /**
     * 删除标记：未删除
     */
    int NOT_DELETED = 0;

    /**
     * 删除标记：已删除
     */
    int DELETED = 1;

    // ==================== 日期格式 ====================

    /**
     * 日期格式：年月日
     */
    String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 日期格式：年月日时分秒
     */
    String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式：年月日（无分隔符）
     */
    String DATE_FORMAT_COMPACT = "yyyyMMdd";

    /**
     * 日期格式：年月日时分秒（无分隔符）
     */
    String DATETIME_FORMAT_COMPACT = "yyyyMMddHHmmss";

    // ==================== 正则表达式 ====================

    /**
     * 手机号正则
     */
    String REGEX_PHONE = "^1[3-9]\\d{9}$";

    /**
     * 邮箱正则
     */
    String REGEX_EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * 身份证号正则（18 位）
     */
    String REGEX_ID_CARD = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    /**
     * 密码正则（8-20位，至少包含字母和数字）
     */
    String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}$";
}
