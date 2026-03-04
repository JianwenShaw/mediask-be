package me.jianwen.mediask.domain.cache;

import java.time.Duration;
import java.util.Objects;

/**
 * 缓存策略定义。
 *
 * <p>每个业务场景通过定义一个 {@code CacheDefinition} 常量来声明其缓存行为。
 * 调用方只需关注"我要缓存什么数据、缓存多久"，无需关心底层实现细节。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 两级缓存：本地 5 分钟 + Redis 1 小时
 * private static final CacheDefinition USER_CACHE = CacheDefinition.builder("user")
 *     .twoLevel()
 *     .localTtl(Duration.ofMinutes(5))
 *     .localMaxSize(2048)
 *     .remoteTtl(Duration.ofHours(1))
 *     .cacheNullValues(Duration.ofSeconds(30))
 *     .build();
 *
 * // 仅远程缓存：适合 Token 等需要跨实例共享的数据
 * private static final CacheDefinition TOKEN_CACHE = CacheDefinition.builder("token")
 *     .remoteOnly()
 *     .remoteTtl(Duration.ofDays(7))
 *     .build();
 *
 * // 仅本地缓存：适合编译产物等不可变数据
 * private static final CacheDefinition DSL_CACHE = CacheDefinition.builder("compiled-dsl")
 *     .localOnly()
 *     .localTtl(Duration.ofMinutes(5))
 *     .localMaxSize(512)
 *     .build();
 * }</pre>
 *
 * @param name            缓存名称，全局唯一，用于指标上报、日志和跨实例失效消息路由
 * @param level           缓存层级策略
 * @param localTtl        本地缓存写入后过期时间（仅 LOCAL / TWO_LEVEL 生效）
 * @param remoteTtl       远程缓存写入后过期时间（仅 REMOTE / TWO_LEVEL 生效）
 * @param localMaxSize    本地缓存最大条目数（仅 LOCAL / TWO_LEVEL 生效）
 * @param cacheNullValues 是否缓存空值以防穿透（回源返回 null 时写入 {@link NullValue} 占位）
 * @param nullValueTtl    空值缓存过期时间（应短于正常 TTL，防止长期占位；仅 cacheNullValues=true 时生效）
 */
public record CacheDefinition(
        String name,
        CacheLevel level,
        Duration localTtl,
        Duration remoteTtl,
        long localMaxSize,
        boolean cacheNullValues,
        Duration nullValueTtl
) {

    /**
     * 校验缓存定义的完整性。
     */
    public CacheDefinition {
        Objects.requireNonNull(name, "缓存名称不能为空");
        if (name.isBlank()) {
            throw new IllegalArgumentException("缓存名称不能为空白");
        }
        Objects.requireNonNull(level, "缓存层级不能为空");

        if (level == CacheLevel.LOCAL || level == CacheLevel.TWO_LEVEL) {
            Objects.requireNonNull(localTtl, "LOCAL / TWO_LEVEL 模式必须指定 localTtl");
            if (localTtl.isNegative() || localTtl.isZero()) {
                throw new IllegalArgumentException("localTtl 必须为正数");
            }
            if (localMaxSize <= 0) {
                throw new IllegalArgumentException("localMaxSize 必须为正数");
            }
        }
        if (level == CacheLevel.REMOTE || level == CacheLevel.TWO_LEVEL) {
            Objects.requireNonNull(remoteTtl, "REMOTE / TWO_LEVEL 模式必须指定 remoteTtl");
            if (remoteTtl.isNegative() || remoteTtl.isZero()) {
                throw new IllegalArgumentException("remoteTtl 必须为正数");
            }
        }
        if (cacheNullValues) {
            Objects.requireNonNull(nullValueTtl, "启用空值缓存时必须指定 nullValueTtl");
            if (nullValueTtl.isNegative() || nullValueTtl.isZero()) {
                throw new IllegalArgumentException("nullValueTtl 必须为正数");
            }
        }
    }

    /**
     * 是否涉及本地缓存层
     */
    public boolean hasLocalLevel() {
        return level == CacheLevel.LOCAL || level == CacheLevel.TWO_LEVEL;
    }

    /**
     * 是否涉及远程缓存层
     */
    public boolean hasRemoteLevel() {
        return level == CacheLevel.REMOTE || level == CacheLevel.TWO_LEVEL;
    }

    /**
     * 创建构建器
     *
     * @param name 缓存名称，全局唯一
     * @return 构建器实例
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * CacheDefinition 构建器。
     *
     * <p>通过 {@link #localOnly()}, {@link #remoteOnly()}, {@link #twoLevel()} 设置层级后，
     * 按需链式配置各项参数。
     */
    public static final class Builder {

        private final String name;
        private CacheLevel level;
        private Duration localTtl;
        private Duration remoteTtl;
        private long localMaxSize;
        private boolean cacheNullValues;
        private Duration nullValueTtl;

        private Builder(String name) {
            this.name = name;
        }

        /** 仅本地缓存 */
        public Builder localOnly() {
            this.level = CacheLevel.LOCAL;
            return this;
        }

        /** 仅远程缓存 */
        public Builder remoteOnly() {
            this.level = CacheLevel.REMOTE;
            return this;
        }

        /** 两级联动缓存 */
        public Builder twoLevel() {
            this.level = CacheLevel.TWO_LEVEL;
            return this;
        }

        /** 本地缓存写入后过期时间 */
        public Builder localTtl(Duration localTtl) {
            this.localTtl = localTtl;
            return this;
        }

        /** 远程缓存写入后过期时间 */
        public Builder remoteTtl(Duration remoteTtl) {
            this.remoteTtl = remoteTtl;
            return this;
        }

        /** 本地缓存最大条目数 */
        public Builder localMaxSize(long localMaxSize) {
            this.localMaxSize = localMaxSize;
            return this;
        }

        /**
         * 启用空值缓存（防穿透）。
         *
         * @param nullValueTtl 空值缓存过期时间，应短于正常 TTL
         */
        public Builder cacheNullValues(Duration nullValueTtl) {
            this.cacheNullValues = true;
            this.nullValueTtl = nullValueTtl;
            return this;
        }

        /** 构建 CacheDefinition 实例 */
        public CacheDefinition build() {
            return new CacheDefinition(name, level, localTtl, remoteTtl, localMaxSize, cacheNullValues, nullValueTtl);
        }
    }
}
