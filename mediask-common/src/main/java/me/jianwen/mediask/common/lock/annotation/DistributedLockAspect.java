package me.jianwen.mediask.common.lock.annotation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.common.lock.DistributedLock;
import me.jianwen.mediask.common.lock.DistributedLockFactory;
import me.jianwen.mediask.common.lock.config.DistributedLockProperties;
import me.jianwen.mediask.common.lock.exception.LockTimeoutException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁 AOP 切面
 * <p>
 * 职责：拦截 @DistributedLockable 注解的方法，自动加锁和解锁
 * 实现原理：
 * 1. 解析 SpEL 表达式生成锁键
 * 2. 尝试获取锁
 * 3. 获取成功：执行业务方法
 * 4. 获取失败：根据策略返回或抛异常
 * 5. finally：释放锁
 * </p>
 *
 * @author jianwen
 * @since 2025-12-14
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAspect {

    private final DistributedLockFactory lockFactory;
    private final DistributedLockProperties properties;

    /**
     * SpEL 表达式解析器
     */
    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * 参数名发现器（用于 SpEL 表达式）
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 环绕通知：拦截 @DistributedLockable 注解的方法
     */
    @Around("@annotation(distributedLockable)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLockable distributedLockable) throws Throwable {
        // 1. 解析锁键（支持 SpEL 表达式）
        String lockKey = parseLockKey(distributedLockable.key(), joinPoint);

        // 2. 获取超时配置
        long waitTime = distributedLockable.waitTime() == -1
                ? properties.getDefaultWaitTime()
                : distributedLockable.waitTime();

        long leaseTime = distributedLockable.leaseTime() == -1
                ? properties.getDefaultLeaseTime()
                : distributedLockable.leaseTime();

        TimeUnit timeUnit = distributedLockable.timeUnit();

        // 3. 创建锁实例
        DistributedLock lock = distributedLockable.fairLock()
                ? lockFactory.createFairLock(lockKey)
                : lockFactory.createLock(lockKey, leaseTime, timeUnit);

        // 4. 尝试获取锁
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                // 获取锁失败：根据策略处理
                return handleLockFailure(distributedLockable, lockKey, waitTime, timeUnit);
            }

            // 5. 获取锁成功：执行业务方法
            log.debug("成功获取分布式锁，开始执行业务: lockKey={}, method={}",
                    lockKey, joinPoint.getSignature().toShortString());

            return joinPoint.proceed();

        } finally {
            // 6. 释放锁（无论成功或失败）
            if (acquired) {
                lock.unlock();
                log.debug("释放分布式锁: lockKey={}", lockKey);
            }
        }
    }

    /**
     * 解析锁键（支持 SpEL 表达式）
     *
     * @param keyExpression SpEL 表达式
     * @param joinPoint     连接点
     * @return 解析后的锁键
     */
    private String parseLockKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        // 如果不包含 SpEL 表达式标识符，直接返回
        if (!keyExpression.contains("#") && !keyExpression.contains("'")) {
            return keyExpression;
        }

        try {
            // 构建 SpEL 上下文
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();

            EvaluationContext context = new StandardEvaluationContext();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 解析表达式
            Expression expression = parser.parseExpression(keyExpression);
            Object value = expression.getValue(context);

            return value != null ? value.toString() : keyExpression;

        } catch (Exception e) {
            log.error("解析锁键 SpEL 表达式失败: keyExpression={}, 使用原始值", keyExpression, e);
            return keyExpression;
        }
    }

    /**
     * 处理获取锁失败的情况
     *
     * @param annotation 注解信息
     * @param lockKey    锁键
     * @param waitTime   等待时间
     * @param timeUnit   时间单位
     * @return 根据策略返回的值
     * @throws LockTimeoutException 获取锁超时异常
     */
    private Object handleLockFailure(DistributedLockable annotation,
            String lockKey,
            long waitTime,
            TimeUnit timeUnit) {
        long timeoutMs = timeUnit.toMillis(waitTime);

        log.warn("获取分布式锁失败: lockKey={}, waitTime={}ms, strategy={}",
                lockKey, timeoutMs, annotation.failStrategy());

        switch (annotation.failStrategy()) {
            case THROW_EXCEPTION:
                throw new LockTimeoutException(lockKey, timeoutMs);
            case RETURN_NULL:
                return null;
            case RETURN_FALSE:
                return false;
            default:
                throw new LockTimeoutException(lockKey, timeoutMs);
        }
    }
}
