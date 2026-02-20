package me.jianwen.mediask.infra.schedule.engine.dsl;

import me.jianwen.mediask.domain.cache.CacheService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * DSL 版本号管理器（用于多实例本地缓存失效协调）。
 */
@Component
public class ConstraintDslVersionManager {

    private final CacheService cacheService;

    public ConstraintDslVersionManager(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public long currentVersion(String namespace, Long departmentId) {
        return cacheService.get(versionKey(namespace, departmentId))
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException ignored) {
                        return 0L;
                    }
                })
                .orElse(0L);
    }

    public long bumpVersion(String namespace, Long departmentId) {
        long next = currentVersion(namespace, departmentId) + 1L;
        cacheService.set(versionKey(namespace, departmentId), String.valueOf(next));
        return next;
    }

    public String versionKey(String namespace, Long departmentId) {
        String safeNamespace = namespace == null || namespace.isBlank()
                ? "DEFAULT"
                : namespace.trim().toUpperCase(Locale.ROOT);
        return "schedule:dsl:version:" + safeNamespace + ":" + departmentId;
    }
}
