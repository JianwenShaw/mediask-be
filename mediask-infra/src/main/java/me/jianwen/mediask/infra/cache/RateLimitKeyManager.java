package me.jianwen.mediask.infra.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 限流键管理器。
 *
 * <p>集中管理所有限流场景的 Redis Key 生成逻辑。
 * 敏感字段（账号、IP）使用 SHA-256 哈希脱敏。
 *
 * <p>注意：缓存键（holiday、token、test-connection 等）已迁移至各自的业务类中，
 * 由 {@link CacheKeyGenerator} 统一加前缀。本类仅保留限流键生成方法。
 */
public final class RateLimitKeyManager {

    private static final String KEY_DELIMITER = ":";
    private static final String RATE_LIMIT_AUTH_LOGIN_ACCOUNT_PREFIX = "rate:limit:auth:login:account";
    private static final String RATE_LIMIT_AUTH_LOGIN_IP_PREFIX = "rate:limit:auth:login:ip";
    private static final String RATE_LIMIT_APPOINTMENT_CREATE_PREFIX = "rate:limit:appointment:create";
    private static final int KEY_HASH_LENGTH = 16;

    private RateLimitKeyManager() {
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
