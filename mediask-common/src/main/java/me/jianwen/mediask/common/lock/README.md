# 分布式锁使用指南

> 基于 Redisson 封装的分布式锁工具，支持编程式和声明式（注解）两种使用方式

## 📦 模块位置

```
mediask-common/
└── src/main/java/me/jianwen/mediask/common/
    ├── lock/                              # 分布式锁包
    │   ├── DistributedLock.java          # 锁接口
    │   ├── DistributedLockFactory.java   # 锁工厂接口
    │   ├── impl/                          # 实现层
    │   │   ├── RedissonDistributedLock.java
    │   │   └── RedissonLockFactory.java
    │   ├── config/                        # 配置层
    │   │   ├── DistributedLockProperties.java
    │   │   └── DistributedLockAutoConfiguration.java
    │   ├── exception/                     # 异常体系
    │   │   ├── LockException.java
    │   │   ├── LockTimeoutException.java
    │   │   └── LockReleaseException.java
    │   ├── annotation/                    # 注解式锁
    │   │   ├── DistributedLockable.java
    │   │   └── DistributedLockAspect.java
    │   └── DistributedLockUsageExample.java
    └── constant/
        └── LockKeyConstant.java           # 锁键常量
```

---

## ⚙️ 配置

### 1. application.yml 配置

```yaml
# Redisson 配置（必需）
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
      database: 0

# 分布式锁配置（可选，使用默认值即可）
mediask:
  lock:
    enabled: true                  # 是否启用分布式锁（默认：true）
    key-prefix: "mediask:lock:"    # 锁键统一前缀（默认：mediask:lock:）
    default-wait-time: 3           # 默认等待时间/秒（默认：3）
    default-lease-time: 30         # 默认租约时间/秒（默认：30，-1 启用看门狗）
    use-fair-lock: false           # 是否使用公平锁（默认：false）
    watchdog-timeout: 10           # 看门狗续期间隔/秒（默认：10）
    enable-metrics: false          # 是否启用性能监控（默认：false）
```

### 2. 自动配置

分布式锁已通过 Spring Boot AutoConfiguration 自动装配，无需手动配置 Bean。

---

## 🚀 使用方式

### 方式一：编程式锁（推荐：精细控制）

#### 1.1 基础用法（try-with-resources）

```java
@Service
@RequiredArgsConstructor
public class AppointmentService {
    
    private final DistributedLockFactory lockFactory;
    
    public void createAppointment(Long scheduleId) {
        String lockKey = LockKeyConstant.APPT_CREATE_PREFIX + scheduleId;
        
        // try-with-resources 自动释放锁
        try (DistributedLock lock = lockFactory.createLock(lockKey)) {
            // 尝试获取锁：等待 3 秒，租约 30 秒
            if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
                throw new BizException("系统繁忙，请稍后重试");
            }
            
            // 业务逻辑：扣减号源
            decreaseSlots(scheduleId);
        }
    }
}
```

#### 1.2 手动释放锁

```java
public void createOrder(String userId) {
    String lockKey = LockKeyConstant.ORDER_CREATE_PREFIX + userId;
    DistributedLock lock = lockFactory.createLock(lockKey);
    
    try {
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new BizException("系统繁忙");
        }
        
        // 业务逻辑
        saveOrder(userId);
        
    } finally {
        lock.unlock(); // 手动释放
    }
}
```

#### 1.3 公平锁（FIFO）

```java
public void updateUser(String userId) {
    String lockKey = LockKeyConstant.USER_UPDATE_PREFIX + userId;
    
    // 创建公平锁：先到先得
    DistributedLock fairLock = lockFactory.createFairLock(lockKey);
    
    try {
        if (!fairLock.tryLock(5, 30, TimeUnit.SECONDS)) {
            throw new BizException("系统繁忙");
        }
        
        // 业务逻辑
        doUpdateUser(userId);
        
    } finally {
        fairLock.unlock();
    }
}
```

#### 1.4 读写锁（读多写少场景）

```java
// 读操作：多个线程可并发执行
public String readCache(String key) {
    DistributedLock readLock = lockFactory.createReadLock(key);
    try {
        if (readLock.tryLock(1, TimeUnit.SECONDS)) {
            return doReadCache(key);
        }
        return null;
    } finally {
        readLock.unlock();
    }
}

// 写操作：独占访问
public void writeCache(String key, String value) {
    DistributedLock writeLock = lockFactory.createWriteLock(key);
    try {
        if (writeLock.tryLock(3, TimeUnit.SECONDS)) {
            doWriteCache(key, value);
        }
    } finally {
        writeLock.unlock();
    }
}
```

---

### 方式二：声明式锁（推荐：简化代码）

#### 2.1 固定锁键

```java
@Service
public class OrderService {
    
    @DistributedLockable(key = "order:create")
    public void createOrder() {
        // 方法执行前自动加锁，执行后自动释放
        saveOrder();
    }
}
```

#### 2.2 SpEL 表达式 - 参数值

```java
@DistributedLockable(
    key = "'appt:' + #scheduleId",
    waitTime = 3,
    leaseTime = 30
)
public void createAppointment(Long scheduleId) {
    // 锁键：appt:123
    decreaseSlots(scheduleId);
}
```

#### 2.3 SpEL 表达式 - 对象属性

```java
@DistributedLockable(
    key = "'user:' + #dto.userId",
    waitTime = 5,
    leaseTime = 60
)
public void updateUser(UserDTO dto) {
    // 锁键：user:1001
    doUpdateUser(dto);
}
```

#### 2.4 多参数拼接

```java
@DistributedLockable(
    key = "'order:' + #userId + ':' + #productId",
    waitTime = 3,
    leaseTime = 30
)
public void createOrder(String userId, String productId) {
    // 锁键：order:user123:product456
    saveOrder(userId, productId);
}
```

#### 2.5 自定义失败策略

```java
// 策略1：抛出异常（默认）
@DistributedLockable(
    key = "'payment:' + #orderId",
    failStrategy = DistributedLockable.LockFailStrategy.THROW_EXCEPTION,
    errorMessage = "支付处理中，请勿重复操作"
)
public void processPayment(String orderId) {
    doPayment(orderId);
}

// 策略2：返回 null
@DistributedLockable(
    key = "'refund:' + #orderId",
    failStrategy = DistributedLockable.LockFailStrategy.RETURN_NULL
)
public String processRefund(String orderId) {
    return "退款成功";
}

// 策略3：返回 false（仅限 boolean 返回值）
@DistributedLockable(
    key = "'check:' + #orderId",
    failStrategy = DistributedLockable.LockFailStrategy.RETURN_FALSE
)
public boolean checkOrder(String orderId) {
    return true;
}
```

#### 2.6 公平锁（注解方式）

```java
@DistributedLockable(
    key = "'queue:' + #userId",
    fairLock = true,
    waitTime = 10,
    leaseTime = 60
)
public void handleRequest(String userId) {
    // FIFO 保证先到先得
    processRequest(userId);
}
```

---

## 🔑 锁键命名规范

使用 `LockKeyConstant` 统一管理锁键前缀：

```java
// 挂号预约
String lockKey = LockKeyConstant.APPT_CREATE_PREFIX + scheduleId;  // appt:create:123

// 用户注册
String lockKey = LockKeyConstant.USER_REGISTER_PREFIX + phone;     // user:register:13800138000

// 订单支付
String lockKey = LockKeyConstant.ORDER_PAY_PREFIX + orderId;       // order:pay:1001

// 库存扣减
String lockKey = LockKeyConstant.INVENTORY_DEDUCT_PREFIX + productId;  // inventory:deduct:456
```

---

## ⏱️ 超时时间设置建议

| 场景 | waitTime | leaseTime | 说明 |
|------|----------|-----------|------|
| **秒杀/抢购** | 1-3s | 10-30s | 快速失败，避免长时间阻塞 |
| **库存扣减** | 3-5s | 30s | 允许短暂等待，防止超卖 |
| **订单支付** | 5-10s | 60s | 支付流程较长，适当延长 |
| **数据同步** | 30s | 300s | 容忍较长等待和执行时间 |
| **缓存重建** | 1s | -1（看门狗）| 快速失败，自动续期 |

---

## ⚠️ 注意事项

### 1. 锁粒度
- ✅ 推荐：细粒度锁（如 `appt:create:123`）
- ❌ 避免：粗粒度锁（如 `appt:create`）

### 2. 租约时间
- **leaseTime = -1**：启用看门狗机制（自动续期），适合执行时间不确定的场景
- **leaseTime > 0**：固定租约时间，适合执行时间可预估的场景
- **建议**：leaseTime 应大于业务执行时间的 2-3 倍

### 3. 异常处理
```java
try (DistributedLock lock = lockFactory.createLock(lockKey)) {
    if (!lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        // 获取锁失败：记录日志，返回友好提示
        log.warn("获取锁失败: lockKey={}", lockKey);
        throw new BizException(ErrorCode.SYSTEM_BUSY);
    }
    
    // 业务逻辑
    
} catch (LockException e) {
    // 锁异常：记录日志，向上抛出
    log.error("分布式锁异常: lockKey={}", lockKey, e);
    throw new SysException(ErrorCode.SYSTEM_ERROR);
}
```

### 4. 日志规范
```java
// ✅ 正确：结构化日志，包含关键信息
log.info("获取分布式锁成功: lockKey={}, waitTime={}ms, leaseTime={}s", 
    lockKey, waitTime, leaseTime);

log.warn("获取分布式锁超时: lockKey={}, timeout={}s, traceId={}", 
    lockKey, timeout, MDC.get("traceId"));

// ❌ 错误：缺少上下文
log.info("锁获取成功");
```

---

## 🎯 最佳实践

### 1. 使用常量管理锁键
```java
// ✅ 推荐
String lockKey = LockKeyConstant.APPT_CREATE_PREFIX + scheduleId;

// ❌ 不推荐
String lockKey = "appt:create:" + scheduleId;
```

### 2. 优先使用 try-with-resources
```java
// ✅ 推荐：自动释放
try (DistributedLock lock = lockFactory.createLock(lockKey)) {
    if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
        // 业务逻辑
    }
}

// ❌ 不推荐：容易忘记释放
DistributedLock lock = lockFactory.createLock(lockKey);
lock.tryLock(3, 30, TimeUnit.SECONDS);
// 业务逻辑
lock.unlock(); // 可能忘记调用
```

### 3. 编程式 vs 声明式选择

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| 需要精确控制锁的获取和释放 | 编程式 | 灵活性高 |
| 整个方法需要加锁 | 声明式 | 代码简洁 |
| 动态锁键（SpEL 表达式）| 声明式 | 表达式解析方便 |
| 需要自定义失败策略 | 声明式 | 配置灵活 |
| 复杂业务逻辑（部分代码加锁）| 编程式 | 控制精确 |

---

## 🐛 常见问题

### Q1: 锁一直获取不到？
**原因**：
1. 其他线程持有锁且未释放
2. 业务执行时间超过 leaseTime，锁已自动释放但业务仍在执行

**解决**：
1. 检查日志，确认锁是否正常释放
2. 增大 leaseTime 或启用看门狗（leaseTime = -1）
3. 优化业务逻辑，减少锁持有时间

### Q2: 锁自动释放了但业务还没执行完？
**原因**：leaseTime 设置过短

**解决**：
1. 增大 leaseTime（建议为业务执行时间的 2-3 倍）
2. 使用看门狗机制（leaseTime = -1）自动续期

### Q3: 注解式锁不生效？
**原因**：
1. 未启用 AOP（检查 `spring-boot-starter-aop` 依赖）
2. 类内部方法调用（AOP 失效）

**解决**：
1. 确保依赖正确
2. 通过 Spring Bean 调用方法，避免 `this.method()` 直接调用

---

## 📚 参考资料

- [Redisson 官方文档](https://github.com/redisson/redisson/wiki)
- [Spring AOP 官方文档](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [分布式锁设计最佳实践](https://redis.io/docs/manual/patterns/distributed-locks/)
