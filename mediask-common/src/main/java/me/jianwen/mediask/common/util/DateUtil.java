package me.jianwen.mediask.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 日期时间工具类
 *
 * @author jianwen
 */
public final class DateUtil {

    /**
     * 默认时区
     */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 日期格式：年-月-日
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 日期时间格式：年-月-日 时:分:秒
     */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 日期格式（紧凑）：年月日
     */
    public static final DateTimeFormatter DATE_COMPACT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 日期时间格式（紧凑）：年月日时分秒
     */
    public static final DateTimeFormatter DATETIME_COMPACT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private DateUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 获取当前日期字符串
     *
     * @return yyyy-MM-dd 格式日期
     */
    public static String nowDateStr() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * 获取当前日期时间字符串
     *
     * @return yyyy-MM-dd HH:mm:ss 格式日期时间
     */
    public static String nowDateTimeStr() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * 获取当前日期字符串（紧凑格式）
     *
     * @return yyyyMMdd 格式日期
     */
    public static String nowDateCompact() {
        return LocalDate.now().format(DATE_COMPACT_FORMATTER);
    }

    /**
     * 获取当前日期时间字符串（紧凑格式）
     *
     * @return yyyyMMddHHmmss 格式日期时间
     */
    public static String nowDateTimeCompact() {
        return LocalDateTime.now().format(DATETIME_COMPACT_FORMATTER);
    }

    /**
     * 解析日期字符串
     *
     * @param dateStr yyyy-MM-dd 格式日期字符串
     * @return LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * 解析日期时间字符串
     *
     * @param dateTimeStr yyyy-MM-dd HH:mm:ss 格式日期时间字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    /**
     * 格式化日期
     *
     * @param date LocalDate
     * @return yyyy-MM-dd 格式日期字符串
     */
    public static String format(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    /**
     * 格式化日期时间
     *
     * @param dateTime LocalDateTime
     * @return yyyy-MM-dd HH:mm:ss 格式日期时间字符串
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    /**
     * Date 转 LocalDateTime
     *
     * @param date Date 对象
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDateTime();
    }

    /**
     * LocalDateTime 转 Date
     *
     * @param localDateTime LocalDateTime 对象
     * @return Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(DEFAULT_ZONE).toInstant());
    }

    /**
     * 时间戳转 LocalDateTime
     *
     * @param timestamp 毫秒时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), DEFAULT_ZONE);
    }

    /**
     * LocalDateTime 转时间戳
     *
     * @param localDateTime LocalDateTime 对象
     * @return 毫秒时间戳
     */
    public static long toTimestamp(LocalDateTime localDateTime) {
        return localDateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * 获取今天开始时间
     *
     * @return 今天 00:00:00
     */
    public static LocalDateTime todayStart() {
        return LocalDate.now().atStartOfDay();
    }

    /**
     * 获取今天结束时间
     *
     * @return 今天 23:59:59
     */
    public static LocalDateTime todayEnd() {
        return LocalDate.now().atTime(23, 59, 59);
    }

    /**
     * 获取指定日期开始时间
     *
     * @param date 日期
     * @return 指定日期 00:00:00
     */
    public static LocalDateTime dayStart(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 获取指定日期结束时间
     *
     * @param date 日期
     * @return 指定日期 23:59:59
     */
    public static LocalDateTime dayEnd(LocalDate date) {
        return date.atTime(23, 59, 59);
    }
}
