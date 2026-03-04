package me.jianwen.mediask.domain.cache;

/**
 * 缓存统计快照。
 *
 * <p>包含本地缓存和远程缓存的命中统计，用于监控和诊断。
 *
 * @param name                缓存名称
 * @param localSize           本地缓存当前条目数
 * @param localHitCount       本地缓存命中次数
 * @param localMissCount      本地缓存未命中次数
 * @param localHitRate        本地缓存命中率（0.0 ~ 1.0）
 * @param localEvictionCount  本地缓存淘汰次数
 * @param remoteHitCount      远程缓存命中次数
 * @param remoteMissCount     远程缓存未命中次数
 */
public record CacheStats(
        String name,
        long localSize,
        long localHitCount,
        long localMissCount,
        double localHitRate,
        long localEvictionCount,
        long remoteHitCount,
        long remoteMissCount
) {

    /**
     * 远程缓存命中率
     */
    public double remoteHitRate() {
        long total = remoteHitCount + remoteMissCount;
        return total == 0 ? 0.0 : (double) remoteHitCount / total;
    }

    /**
     * 创建一个空统计快照
     */
    public static CacheStats empty(String name) {
        return new CacheStats(name, 0, 0, 0, 0.0, 0, 0, 0);
    }
}
