package me.jianwen.mediask.domain.cache;

/**
 * 缓存层级策略。
 *
 * <p>决定缓存数据存放在哪一层：
 * <ul>
 *   <li>{@link #LOCAL} — 仅进程内缓存（Caffeine），适合不可变或变更极少的数据</li>
 *   <li>{@link #REMOTE} — 仅远程缓存（Redis），适合需要跨实例共享但无需极低延迟的数据</li>
 *   <li>{@link #TWO_LEVEL} — 两级联动（L1 Caffeine + L2 Redis），适合高频读取且允许短暂不一致的热数据</li>
 * </ul>
 */
public enum CacheLevel {

    /**
     * 仅本地缓存（进程内，Caffeine）。
     * <p>
     * 特点：读取延迟最低（纳秒级），但多实例间不共享数据。
     * 适用场景：编译结果缓存、正则表达式缓存等不可变数据。
     */
    LOCAL,

    /**
     * 仅远程缓存（Redis）。
     * <p>
     * 特点：多实例共享，强一致性，读取延迟毫秒级。
     * 适用场景：Token 存储、分布式 Session、需要实时失效的数据。
     */
    REMOTE,

    /**
     * 两级联动缓存（L1 Caffeine + L2 Redis）。
     * <p>
     * 读取链路：L1 命中 → 返回；L1 未命中 → L2 命中 → 回填 L1 并返回；全部未命中 → 回源加载 → 回填 L1+L2。
     * 写入/失效：删除 L1 + L2，并通过 Pub/Sub 广播失效消息到所有实例的 L1。
     * 适用场景：节假日查询、科室信息、医院配置等高频读取的热数据。
     */
    TWO_LEVEL
}
