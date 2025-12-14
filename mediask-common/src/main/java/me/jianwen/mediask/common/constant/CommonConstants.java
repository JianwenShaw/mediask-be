package me.jianwen.mediask.common.constant;

/**
 * 公共常量定义
 * <p>
 * 设计说明：使用 final class 而非 interface，符合《Effective Java》规范
 * </p>
 *
 * @author jianwen
 */
public final class CommonConstants {

    /**
     * 私有构造器，防止实例化
     */
    private CommonConstants() {
        throw new UnsupportedOperationException("Constant class cannot be instantiated");
    }

    // ==================== 系统常量 ====================

    /**
     * 系统名称
     */
    public static final String SYSTEM_NAME = "MediAsk";

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 默认起始页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;

    // ==================== 请求头常量 ====================

    /**
     * Token 请求头
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Token 前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 追踪 ID 请求头
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    /**
     * 请求 ID 请求头
     */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /**
     * 幂等性 Key 请求头
     */
    public static final String HEADER_IDEMPOTENT_KEY = "X-Idempotent-Key";

    /**
     * 客户端类型请求头
     */
    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";

    // ==================== MDC 日志上下文 ====================

    /**
     * 追踪 ID（MDC）
     */
    public static final String MDC_TRACE_ID = "traceId";

    /**
     * 用户 ID（MDC）
     */
    public static final String MDC_USER_ID = "userId";

    /**
     * 请求 URI（MDC）
     */
    public static final String MDC_REQUEST_URI = "requestUri";

    // ==================== 状态常量 ====================

    /**
     * 正常状态
     */
    public static final int STATUS_NORMAL = 1;

    /**
     * 禁用状态
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 删除标记：未删除
     */
    public static final int NOT_DELETED = 0;

    /**
     * 删除标记：已删除
     */
    public static final int DELETED = 1;

    // ==================== 日期格式 ====================

    /**
     * 日期格式：年月日
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 日期格式：年月日时分秒
     */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式：年月日（无分隔符）
     */
    public static final String DATE_FORMAT_COMPACT = "yyyyMMdd";

    /**
     * 日期格式：年月日时分秒（无分隔符）
     */
    public static final String DATETIME_FORMAT_COMPACT = "yyyyMMddHHmmss";

    // ==================== 正则表达式 ====================

    /**
     * 手机号正则
     */
    public static final String REGEX_PHONE = "^1[3-9]\\d{9}$";

    /**
     * 邮箱正则
     */
    public static final String REGEX_EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * 身份证号正则（18 位）
     */
    public static final String REGEX_ID_CARD = "^[1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    /**
     * 密码正则（8-20位，至少包含字母和数字）
     */
    public static final String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}$";
}
