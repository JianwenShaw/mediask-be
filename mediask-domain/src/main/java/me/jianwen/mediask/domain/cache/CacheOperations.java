package me.jianwen.mediask.domain.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 统一缓存操作接口。
 *
 * <p>调用方无需关心底层缓存层级（L1 本地 / L2 远程），所有读写行为由
 * {@link CacheDefinition} 声明的策略自动编排。
 *
 * <h3>核心读取链路（TWO_LEVEL 模式）</h3>
 * <pre>
 *   L1 (Caffeine) 命中 → 直接返回
 *        ↓ 未命中
 *   L2 (Redis) 命中 → 回填 L1 → 返回
 *        ↓ 未命中
 *   Loader 回源加载 → 回填 L2 + L1 → 返回
 * </pre>
 *
 * <h3>失效广播</h3>
 * <p>调用 {@link #evict} / {@link #evictAll} 时，自动通过 Redis Pub/Sub
 * 广播失效消息到所有实例的 L1 缓存，无需业务方手动处理。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class UserService {
 *
 *     private static final CacheDefinition USER_CACHE = CacheDefinition.builder("user")
 *         .twoLevel()
 *         .localTtl(Duration.ofMinutes(5))
 *         .localMaxSize(2048)
 *         .remoteTtl(Duration.ofHours(1))
 *         .cacheNullValues(Duration.ofSeconds(30))
 *         .build();
 *
 *     private final CacheOperations cacheOperations;
 *
 *     public UserDTO getUser(Long userId) {
 *         return cacheOperations.get(USER_CACHE, "user:" + userId, UserDTO.class,
 *             () -> userRepository.findById(userId).orElse(null));
 *     }
 *
 *     public void updateUser(Long userId, UpdateUserRequest request) {
 *         userRepository.update(userId, request);
 *         cacheOperations.evict(USER_CACHE, "user:" + userId);
 *     }
 * }
 * }</pre>
 */
public interface CacheOperations {

    /**
     * 读取缓存，未命中时通过 loader 加载并回填所有缓存层。
     *
     * @param definition 缓存策略定义
     * @param key        缓存键（不含全局前缀，由实现层自动拼接）
     * @param type       值的 Class（用于反序列化）
     * @param loader     回源加载器，缓存全部未命中时调用
     * @param <T>        值类型
     * @return 缓存值；若回源也为 null 且未启用空值缓存，返回 null
     */
    <T> T get(CacheDefinition definition, String key, Class<T> type, Supplier<T> loader);

    /**
     * 读取缓存（支持泛型集合等复杂类型的反序列化）。
     *
     * <p>例如 {@code new TypeReference<List<UserDTO>>() {}} 用于反序列化列表类型。
     *
     * @param definition    缓存策略定义
     * @param key           缓存键
     * @param typeReference 类型引用（解决泛型擦除问题）
     * @param loader        回源加载器
     * @param <T>           值类型
     * @return 缓存值
     */
    <T> T get(CacheDefinition definition, String key, TypeReference<T> typeReference, Supplier<T> loader);

    /**
     * 直接写入缓存（覆盖所有层的已有值）。
     *
     * @param definition 缓存策略定义
     * @param key        缓存键
     * @param value      缓存值（不能为 null，如需缓存空值请在 CacheDefinition 中启用 cacheNullValues）
     * @param <T>        值类型
     */
    <T> void put(CacheDefinition definition, String key, T value);

    /**
     * 删除缓存（所有层），并广播失效消息到其他实例的 L1 缓存。
     *
     * @param definition 缓存策略定义
     * @param key        缓存键
     */
    void evict(CacheDefinition definition, String key);

    /**
     * 删除指定 definition 下的所有缓存。
     *
     * <p>注意：远程缓存（Redis）的清除依赖 SCAN 操作，大量 Key 时可能有延迟。
     *
     * @param definition 缓存策略定义
     */
    void evictAll(CacheDefinition definition);

    /**
     * 仅读取缓存（不触发回源加载）。
     *
     * @param definition 缓存策略定义
     * @param key        缓存键
     * @param type       值的 Class
     * @param <T>        值类型
     * @return 缓存值，不存在则返回 {@link Optional#empty()}
     */
    <T> Optional<T> getIfPresent(CacheDefinition definition, String key, Class<T> type);

    /**
     * 判断缓存键是否存在（任意层命中即返回 true）。
     *
     * @param definition 缓存策略定义
     * @param key        缓存键
     * @return 是否存在
     */
    boolean exists(CacheDefinition definition, String key);
}
