package me.jianwen.mediask.infra.schedule.engine.dsl;

import me.jianwen.mediask.infra.cache.RedisCacheAdapter;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * DSL 版本号管理器（用于多实例本地缓存失效协调）。
 *
 * <p>每次约束规则变更时调用 {@link #bumpVersion}，编译器会根据版本号变化
 * 自动失效本地编译缓存。
 *
 * <p>使用 Redis {@code INCR} 原子递增，修复旧版 GET-then-SET 竞态条件。
 */
@Component
public class ConstraintDslVersionManager {

    /**
     * 版本号键前缀（直接存储在 Redis 中，不走缓存前缀）
     */
    private static final String VERSION_KEY_PREFIX = "schedule:dsl:version:";

    private final RedisCacheAdapter redisCacheAdapter;

    public ConstraintDslVersionManager(RedisCacheAdapter redisCacheAdapter) {
        this.redisCacheAdapter = redisCacheAdapter;
    }

    /**
     * 获取当前版本号。
     *
     * @param namespace    命名空间
     * @param departmentId 科室ID
     * @return 当前版本号，不存在返回 0
     */
    public long currentVersion(String namespace, Long departmentId) {
        String raw = redisCacheAdapter.getRaw(versionKey(namespace, departmentId));
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    /**
     * 原子递增版本号。
     *
     * <p>使用 Redis INCR 保证多实例并发调用时版本号严格递增，不会回退。
     *
     * @param namespace    命名空间
     * @param departmentId 科室ID
     * @return 递增后的版本号
     */
    public long bumpVersion(String namespace, Long departmentId) {
        return redisCacheAdapter.increment(versionKey(namespace, departmentId));
    }

    public String versionKey(String namespace, Long departmentId) {
        String safeNamespace = namespace == null || namespace.isBlank()
                ? "DEFAULT"
                : namespace.trim().toUpperCase(Locale.ROOT);
        return VERSION_KEY_PREFIX + safeNamespace + ":" + departmentId;
    }
}
