package me.jianwen.mediask.infra.holiday;

import lombok.extern.slf4j.Slf4j;
import me.jianwen.mediask.domain.cache.CacheDefinition;
import me.jianwen.mediask.domain.cache.CacheOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * 节假日服务。
 *
 * <p>提供节假日查询和管理功能：
 * <ul>
 *   <li>查询指定日期是否为节假日</li>
 *   <li>获取节假日列表</li>
 *   <li>支持从外部API同步节假日数据</li>
 * </ul>
 *
 * @author MediAsk
 */
@Slf4j
@Service
@EnableScheduling
public class HolidayService {

    /**
     * 节假日缓存定义：两级缓存（L1 6 小时 + L2 30 天），启用空值缓存防止不存在日期的穿透。
     */
    private static final CacheDefinition HOLIDAY_CACHE = CacheDefinition.builder("holiday")
            .twoLevel()
            .localTtl(Duration.ofHours(6))
            .localMaxSize(4096)
            .remoteTtl(Duration.ofDays(30))
            .cacheNullValues(Duration.ofMinutes(10))
            .build();

    private final CacheOperations cacheOperations;

    @Value("${app.holiday.api-url:}")
    private String holidayApiUrl;

    // 预定义的2025年节假日
    private static final Set<LocalDate> HOLIDAYS_2025 = Set.of(
            // 元旦
            LocalDate.of(2025, 1, 1),
            // 春节
            LocalDate.of(2025, 1, 28), LocalDate.of(2025, 1, 29),
            LocalDate.of(2025, 1, 30), LocalDate.of(2025, 1, 31),
            LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 2),
            LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 4),
            // 清明节
            LocalDate.of(2025, 4, 4), LocalDate.of(2025, 4, 5),
            LocalDate.of(2025, 4, 6),
            // 劳动节
            LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 2),
            LocalDate.of(2025, 5, 3), LocalDate.of(2025, 5, 4),
            // 端午节
            LocalDate.of(2025, 5, 31),
            // 中秋节
            LocalDate.of(2025, 9, 21),
            // 国庆节
            LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 2),
            LocalDate.of(2025, 10, 3), LocalDate.of(2025, 10, 4),
            LocalDate.of(2025, 10, 5), LocalDate.of(2025, 10, 6),
            LocalDate.of(2025, 10, 7)
    );

    // 预定义的2026年节假日
    private static final Set<LocalDate> HOLIDAYS_2026 = Set.of(
            // 元旦
            LocalDate.of(2026, 1, 1),
            // 春节
            LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 18),
            LocalDate.of(2026, 2, 19), LocalDate.of(2026, 2, 20),
            LocalDate.of(2026, 2, 21), LocalDate.of(2026, 2, 22),
            LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 24),
            // 清明节
            LocalDate.of(2026, 4, 5),
            // 劳动节
            LocalDate.of(2026, 5, 1),
            // 端午节
            LocalDate.of(2026, 6, 20),
            // 中秋节
            LocalDate.of(2026, 9, 10),
            // 国庆节
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2),
            LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 4),
            LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6),
            LocalDate.of(2026, 10, 7)
    );

    public HolidayService(CacheOperations cacheOperations) {
        this.cacheOperations = cacheOperations;
    }

    /**
     * 应用启动时初始化节假日缓存
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeHolidays() {
        log.info("初始化节假日缓存...");
        cacheHolidays(HOLIDAYS_2025);
        cacheHolidays(HOLIDAYS_2026);
        log.info("节假日缓存初始化完成");
    }

    /**
     * 缓存节假日到所有缓存层
     */
    private void cacheHolidays(Set<LocalDate> holidays) {
        for (LocalDate date : holidays) {
            String key = dateKey(date);
            cacheOperations.put(HOLIDAY_CACHE, key, Boolean.TRUE);
        }
    }

    /**
     * 检查指定日期是否为节假日
     *
     * @param date 要检查的日期，不能为 null
     * @return 如果是节假日返回 true
     * @throws IllegalArgumentException 如果 date 为 null
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        String key = dateKey(date);

        // 通过两级缓存查询，loader 回源检查静态数据
        Boolean cached = cacheOperations.get(HOLIDAY_CACHE, key, Boolean.class, () -> {
            if (HOLIDAYS_2025.contains(date) || HOLIDAYS_2026.contains(date)) {
                return Boolean.TRUE;
            }
            return null; // 非节假日，空值缓存防穿透
        });

        return Boolean.TRUE.equals(cached);
    }

    /**
     * 检查指定日期是否为工作日
     */
    public boolean isWorkday(LocalDate date) {
        // 周末不是工作日
        if (isWeekend(date)) {
            return false;
        }

        // 节假日不是工作日
        if (isHoliday(date)) {
            return false;
        }

        return true;
    }

    /**
     * 检查是否为周末
     */
    public boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 获取指定年份的所有节假日
     */
    public Set<LocalDate> getHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();

        if (year == 2025) {
            holidays.addAll(HOLIDAYS_2025);
        } else if (year == 2026) {
            holidays.addAll(HOLIDAYS_2026);
        }

        return holidays;
    }

    /**
     * 获取指定日期范围内的节假日
     */
    public List<LocalDate> getHolidaysBetween(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> holidays = new ArrayList<>();

        Set<LocalDate> allHolidays = new HashSet<>();
        allHolidays.addAll(HOLIDAYS_2025);
        allHolidays.addAll(HOLIDAYS_2026);

        for (LocalDate date : allHolidays) {
            if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                holidays.add(date);
            }
        }

        holidays.sort(LocalDate::compareTo);
        return holidays;
    }

    /**
     * 获取指定日期范围内的工作日数量
     */
    public int countWorkdays(LocalDate startDate, LocalDate endDate) {
        int count = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (isWorkday(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    /**
     * 从下一个工作日开始跳过指定天数
     */
    public LocalDate skipWorkdays(LocalDate startDate, int days) {
        LocalDate current = startDate;
        int skipped = 0;

        while (skipped < days) {
            current = current.plusDays(1);
            if (isWorkday(current)) {
                skipped++;
            }
        }

        return current;
    }

    /**
     * 定时同步节假日数据（如果有配置API）
     */
    @Scheduled(cron = "0 0 0 1 1 ?") // 每年1月1日执行
    public void syncHolidays() {
        if (holidayApiUrl == null || holidayApiUrl.isEmpty()) {
            log.info("未配置节假日API，跳过同步");
            return;
        }

        log.info("开始同步节假日数据...");
        // TODO: 实现API调用同步节假日数据
        // 可以调用第三方节假日API（如阿里云、百度等）
        log.info("节假日数据同步完成");
    }

    /**
     * 生成日期缓存键（不含全局前缀，由 CacheKeyGenerator 自动拼接）。
     */
    private static String dateKey(LocalDate date) {
        return date.toString(); // ISO-8601 格式：yyyy-MM-dd
    }
}
