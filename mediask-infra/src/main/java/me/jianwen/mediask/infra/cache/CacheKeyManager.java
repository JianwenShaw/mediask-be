package me.jianwen.mediask.infra.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 统一缓存 Key 管理
 */
public final class CacheKeyManager {

    private static final String KEY_DELIMITER = ":";
    private static final String PATTERN_SUFFIX = "*";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String AUTH_REFRESH_PREFIX = "auth:refresh";
    private static final String HOLIDAY_PREFIX = "holiday";
    private static final String TEST_CONNECTION_PREFIX = "test:connection";
    private static final String RATE_LIMIT_AUTH_LOGIN_ACCOUNT_PREFIX = "rate:limit:auth:login:account";
    private static final String RATE_LIMIT_AUTH_LOGIN_IP_PREFIX = "rate:limit:auth:login:ip";
    private static final String RATE_LIMIT_APPOINTMENT_CREATE_PREFIX = "rate:limit:appointment:create";
    private static final int KEY_HASH_LENGTH = 16;

    private CacheKeyManager() {
    }

    public static String refreshTokenKey(Long userId, String tokenId) {
        return join(AUTH_REFRESH_PREFIX, String.valueOf(userId), tokenId);
    }

    public static String refreshTokenPattern(Long userId) {
        return join(AUTH_REFRESH_PREFIX, String.valueOf(userId), PATTERN_SUFFIX);
    }

    public static String holidayKey(LocalDate date) {
        return join(HOLIDAY_PREFIX, DATE_FORMATTER.format(date));
    }

    public static String testConnectionKey(String key) {
        return join(TEST_CONNECTION_PREFIX, key);
    }

    public static String authLoginAccountRateLimitKey(String account) {
        String normalizedAccount = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        String accountHash = sha256Hex(normalizedAccount);
        return join(RATE_LIMIT_AUTH_LOGIN_ACCOUNT_PREFIX, accountHash.substring(0, KEY_HASH_LENGTH));
    }

    public static String authLoginIpRateLimitKey(String clientIp) {
        String normalizedIp = clientIp == null ? "" : clientIp.trim().toLowerCase(Locale.ROOT);
        String ipHash = sha256Hex(normalizedIp);
        return join(RATE_LIMIT_AUTH_LOGIN_IP_PREFIX, ipHash.substring(0, KEY_HASH_LENGTH));
    }

    public static String appointmentCreateRateLimitKey(Long patientId) {
        return join(RATE_LIMIT_APPOINTMENT_CREATE_PREFIX, String.valueOf(patientId));
    }

    private static String join(String... parts) {
        return String.join(KEY_DELIMITER, parts);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
