package me.jianwen.mediask.common.constant;

/**
 * Redis Key 常量定义
 * <p>
 * 统一管理 Redis Key 前缀，遵循 app:module:bizId 命名规范
 * </p>
 *
 * @author jianwen
 */
public interface RedisKeyConstant {

    /**
     * 应用前缀
     */
    String PREFIX = "mediask:";

    // ==================== 分布式锁 ====================

    /**
     * 挂号分布式锁前缀
     * 完整 Key: mediask:dlock:appt:{scheduleId}
     */
    String APPT_LOCK = PREFIX + "dlock:appt:";

    /**
     * 库存扣减锁前缀
     * 完整 Key: mediask:dlock:stock:{scheduleId}
     */
    String STOCK_LOCK = PREFIX + "dlock:stock:";

    /**
     * 用户操作锁前缀（防重复提交）
     * 完整 Key: mediask:dlock:user:{userId}:{operation}
     */
    String USER_LOCK = PREFIX + "dlock:user:";

    // ==================== 序列号生成 ====================

    /**
     * 挂号序列号
     * 完整 Key: mediask:seq:appt:{date}
     */
    String APPT_SEQ = PREFIX + "seq:appt:";

    /**
     * 病历序列号
     * 完整 Key: mediask:seq:emr:{date}
     */
    String EMR_SEQ = PREFIX + "seq:emr:";

    // ==================== 缓存 ====================

    /**
     * 医生排班缓存
     * 完整 Key: mediask:schedule:doctor:{doctorId}:{date}
     */
    String SCHEDULE_CACHE = PREFIX + "schedule:doctor:%s:%s";

    /**
     * 科室信息缓存
     * 完整 Key: mediask:cache:dept:{deptId}
     */
    String DEPT_CACHE = PREFIX + "cache:dept:";

    /**
     * 医院信息缓存
     * 完整 Key: mediask:cache:hospital:{hospitalId}
     */
    String HOSPITAL_CACHE = PREFIX + "cache:hospital:";

    /**
     * 用户信息缓存
     * 完整 Key: mediask:cache:user:{userId}
     */
    String USER_CACHE = PREFIX + "cache:user:";

    // ==================== Token & 会话 ====================

    /**
     * 用户 Token
     * 完整 Key: mediask:token:user:{userId}
     */
    String USER_TOKEN = PREFIX + "token:user:";

    /**
     * Token 黑名单
     * 完整 Key: mediask:token:blacklist:{token}
     */
    String TOKEN_BLACKLIST = PREFIX + "token:blacklist:";

    /**
     * 刷新 Token
     * 完整 Key: mediask:token:refresh:{userId}
     */
    String REFRESH_TOKEN = PREFIX + "token:refresh:";

    // ==================== 验证码 ====================

    /**
     * 短信验证码
     * 完整 Key: mediask:captcha:sms:{phone}
     */
    String SMS_CAPTCHA = PREFIX + "captcha:sms:";

    /**
     * 图形验证码
     * 完整 Key: mediask:captcha:image:{uuid}
     */
    String IMAGE_CAPTCHA = PREFIX + "captcha:image:";

    /**
     * 邮箱验证码
     * 完整 Key: mediask:captcha:email:{email}
     */
    String EMAIL_CAPTCHA = PREFIX + "captcha:email:";

    // ==================== 限流 ====================

    /**
     * 接口限流
     * 完整 Key: mediask:rate:api:{uri}:{userId}
     */
    String API_RATE_LIMIT = PREFIX + "rate:api:";

    /**
     * 短信发送限流
     * 完整 Key: mediask:rate:sms:{phone}
     */
    String SMS_RATE_LIMIT = PREFIX + "rate:sms:";

    /**
     * 登录失败次数
     * 完整 Key: mediask:rate:login:{username}
     */
    String LOGIN_FAIL_COUNT = PREFIX + "rate:login:";

    // ==================== 幂等性 ====================

    /**
     * 幂等性 Key
     * 完整 Key: mediask:idempotent:{bizType}:{bizKey}
     */
    String IDEMPOTENT = PREFIX + "idempotent:";

    // ==================== 过期时间常量（秒） ====================

    /**
     * 缓存过期时间（1 天）
     */
    long CACHE_EXPIRE_SECONDS = 24 * 60 * 60;

    /**
     * 短期缓存过期时间（30 分钟）
     */
    long SHORT_CACHE_EXPIRE_SECONDS = 30 * 60;

    /**
     * 验证码过期时间（5 分钟）
     */
    long CAPTCHA_EXPIRE_SECONDS = 5 * 60;

    /**
     * Token 过期时间（7 天）
     */
    long TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 刷新 Token 过期时间（30 天）
     */
    long REFRESH_TOKEN_EXPIRE_SECONDS = 30 * 24 * 60 * 60;

    /**
     * 分布式锁等待时间（秒）
     */
    long LOCK_WAIT_SECONDS = 3;

    /**
     * 分布式锁持有时间（秒）
     */
    long LOCK_LEASE_SECONDS = 10;
}
