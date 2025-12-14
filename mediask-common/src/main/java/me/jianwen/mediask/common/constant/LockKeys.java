package me.jianwen.mediask.common.constant;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 分布式锁键枚举
 * <p>
 * 职责：统一管理所有业务场景的锁键定义
 * 设计优势：
 * 1. 类型安全：编译期检查，防止拼写错误
 * 2. 行为封装：提供 buildKey() 方法自动拼接参数
 * 3. 元数据管理：关联描述、分类、推荐超时时间
 * 4. IDE 友好：自动补全和重构支持
 * </p>
 * <p>
 * 使用示例：
 *
 * <pre>
 * // 方式1：构建锁键
 * String lockKey = LockKeys.APPT_CREATE.buildKey(scheduleId);
 *
 * // 方式2：获取前缀（兼容旧代码）
 * String prefix = LockKeys.APPT_CREATE.getPrefix();
 *
 * // 方式3：使用推荐超时时间
 * int waitTime = LockKeys.APPT_CREATE.getRecommendedWaitTime();
 * int leaseTime = LockKeys.APPT_CREATE.getRecommendedLeaseTime();
 * </pre>
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
public enum LockKeys {

    // ==================== 挂号预约模块 ====================

    /**
     * 挂号创建锁
     * 场景：防止同一排班被超卖
     * 锁键格式：appt:create:{scheduleId}
     */
    APPT_CREATE("appt:create:", "挂号创建锁", LockCategory.APPOINTMENT, 3, 30),

    /**
     * 挂号取消锁
     * 场景：防止重复取消
     * 锁键格式：appt:cancel:{apptId}
     */
    APPT_CANCEL("appt:cancel:", "挂号取消锁", LockCategory.APPOINTMENT, 3, 10),

    /**
     * 号源刷新锁
     * 场景：定时任务刷新号源时加锁
     * 锁键格式：appt:refresh:{scheduleId}
     */
    APPT_REFRESH("appt:refresh:", "号源刷新锁", LockCategory.APPOINTMENT, 5, 60),

    // ==================== 用户模块 ====================

    /**
     * 用户注册锁
     * 场景：防止同一手机号重复注册
     * 锁键格式：user:register:{phone}
     */
    USER_REGISTER("user:register:", "用户注册锁", LockCategory.USER, 3, 30),

    /**
     * 用户登录锁
     * 场景：防止暴力破解（同一账号短时间多次登录）
     * 锁键格式：user:login:{userId}
     */
    USER_LOGIN("user:login:", "用户登录锁", LockCategory.USER, 1, 10),

    /**
     * 用户信息更新锁
     * 场景：防止并发更新用户信息
     * 锁键格式：user:update:{userId}
     */
    USER_UPDATE("user:update:", "用户信息更新锁", LockCategory.USER, 5, 30),

    // ==================== 订单模块 ====================

    /**
     * 订单创建锁
     * 场景：防止重复下单
     * 锁键格式：order:create:{userId}
     */
    ORDER_CREATE("order:create:", "订单创建锁", LockCategory.ORDER, 3, 30),

    /**
     * 订单支付锁
     * 场景：防止重复支付
     * 锁键格式：order:pay:{orderId}
     */
    ORDER_PAY("order:pay:", "订单支付锁", LockCategory.ORDER, 5, 60),

    /**
     * 订单退款锁
     * 场景：防止重复退款
     * 锁键格式：order:refund:{orderId}
     */
    ORDER_REFUND("order:refund:", "订单退款锁", LockCategory.ORDER, 5, 60),

    // ==================== 库存模块 ====================

    /**
     * 库存扣减锁
     * 场景：防止库存超卖
     * 锁键格式：inventory:deduct:{productId}
     */
    INVENTORY_DEDUCT("inventory:deduct:", "库存扣减锁", LockCategory.INVENTORY, 3, 30),

    /**
     * 库存回滚锁
     * 场景：订单取消时回滚库存
     * 锁键格式：inventory:rollback:{productId}
     */
    INVENTORY_ROLLBACK("inventory:rollback:", "库存回滚锁", LockCategory.INVENTORY, 3, 30),

    // ==================== 支付模块 ====================

    /**
     * 支付回调锁
     * 场景：防止支付回调重复处理
     * 锁键格式：payment:callback:{outTradeNo}
     */
    PAYMENT_CALLBACK("payment:callback:", "支付回调锁", LockCategory.PAYMENT, 5, 60),

    // ==================== 缓存模块 ====================

    /**
     * 缓存重建锁
     * 场景：防止缓存击穿（同一时刻多个请求重建缓存）
     * 锁键格式：cache:rebuild:{cacheKey}
     */
    CACHE_REBUILD("cache:rebuild:", "缓存重建锁", LockCategory.CACHE, 1, -1),

    // ==================== 定时任务模块 ====================

    /**
     * 定时任务执行锁
     * 场景：分布式定时任务防止重复执行
     * 锁键格式：job:{jobName}
     */
    JOB_EXECUTE("job:", "定时任务执行锁", LockCategory.JOB, 30, 300),

    // ==================== AI 模块 ====================

    /**
     * AI 问诊锁
     * 场景：限制用户同时只能有一个问诊会话
     * 锁键格式：ai:consult:{userId}
     */
    AI_CONSULT("ai:consult:", "AI问诊锁", LockCategory.AI, 3, 60);

    // ==================== 枚举字段 ====================

    /**
     * 锁键前缀
     */
    private final String prefix;

    /**
     * 锁描述
     */
    private final String description;

    /**
     * 锁分类
     */
    private final LockCategory category;

    /**
     * 推荐等待时间（秒）
     */
    private final int recommendedWaitTime;

    /**
     * 推荐租约时间（秒）
     * -1 表示启用看门狗自动续期
     */
    private final int recommendedLeaseTime;

    // ==================== 构造器 ====================

    LockKeyType(String prefix, String description, LockCategory category,
            int recommendedWaitTime, int recommendedLeaseTime) {
        this.prefix = prefix;
        this.description = description;
        this.category = category;
        this.recommendedWaitTime = recommendedWaitTime;
        this.recommendedLeaseTime = recommendedLeaseTime;
    }

    // ==================== 公共方法 ====================

    /**
     * 构建完整锁键
     * <p>
     * 示例：
     * 
     * <pre>
     * LockKeys.APPT_CREATE.buildKey(123)           → "appt:create:123"
     * LockKeys.ORDER_CREATE.buildKey("u1", "p2")  → "order:create:u1:p2"
     * </pre>
     * </p>
     *
     * @param params 锁键参数（可变参数）
     * @return 完整锁键
     */
    public String buildKey(Object... params) {
        if (params == null || params.length == 0) {
            return prefix.substring(0, prefix.length() - 1); // 去掉末尾的 ':'
        }
        return prefix + Arrays.stream(params)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
    }

    /**
     * 获取锁键前缀（兼容旧代码）
     *
     * @return 锁键前缀
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 获取锁描述
     *
     * @return 锁描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取锁分类
     *
     * @return 锁分类
     */
    public LockCategory getCategory() {
        return category;
    }

    /**
     * 获取推荐等待时间（秒）
     *
     * @return 等待时间
     */
    public int getRecommendedWaitTime() {
        return recommendedWaitTime;
    }

    /**
     * 获取推荐租约时间（秒）
     *
     * @return 租约时间
     */
    public int getRecommendedLeaseTime() {
        return recommendedLeaseTime;
    }

    // ==================== 内部枚举：锁分类 ====================

    /**
     * 锁分类枚举
     * 用于统计和监控
     */
    public enum LockCategory {
        /**
         * 挂号预约
         */
        APPOINTMENT("挂号预约"),

        /**
         * 用户管理
         */
        USER("用户管理"),

        /**
         * 订单管理
         */
        ORDER("订单管理"),

        /**
         * 库存管理
         */
        INVENTORY("库存管理"),

        /**
         * 支付管理
         */
        PAYMENT("支付管理"),

        /**
         * 缓存管理
         */
        CACHE("缓存管理"),

        /**
         * 定时任务
         */
        JOB("定时任务"),

        /**
         * AI 服务
         */
        AI("AI服务");

        private final String description;

        LockCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
