package me.jianwen.mediask.api.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import me.jianwen.mediask.infra.config.JwtProperties;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT Token 解码工具类
 * 用于调试时查看 Token 中的 payload 内容（不验证签名）
 */
public class JwtDecoderUtils {

    private JwtDecoderUtils() {
    }

    /**
     * 解码 JWT Token（仅解析内容，不验证签名）
     *
     * @param token JWT 字符串
     * @param secret JWT 密钥
     * @return 解码后的 payload 信息
     */
    public static DecodedToken decode(String token, String secret) {
        try {
            var signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.getSubject() != null ? Long.parseLong(claims.getSubject()) : null;
            String username = claims.get("username", String.class);
            Integer userType = claims.get("userType", Integer.class);
            @SuppressWarnings("unchecked")
            List<String> authorities = claims.get("perms", List.class);
            String tokenKind = claims.get("tokenKind", String.class);
            String tokenId = claims.get("jti", String.class);
            Date issuedAt = claims.getIssuedAt();
            Date expiresAt = claims.getExpiration();

            return new DecodedToken(
                    userId,
                    username,
                    userType,
                    authorities,
                    tokenKind,
                    tokenId,
                    issuedAt,
                    expiresAt
            );
        } catch (Exception e) {
            throw new RuntimeException("Token 解码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解码 JWT Token（不验证签名，仅 Base64 解码）
     * 用于快速查看内容，不要求密钥
     *
     * @param token JWT 字符串
     * @return 解码后的 payload 信息
     */
    public static Map<String, Object> decodeWithoutVerify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Token 解码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 打印 Token 详细信息
     *
     * @param token JWT 字符串
     * @param secret JWT 密钥
     */
    public static void printTokenInfo(String token, String secret) {
        System.out.println("=".repeat(60));
        System.out.println("JWT Token 解析结果");
        System.out.println("=".repeat(60));

        try {
            DecodedToken decoded = decode(token, secret);

            System.out.println("User ID:     " + decoded.userId());
            System.out.println("Username:    " + decoded.username());
            System.out.println("User Type:   " + decoded.userType());
            System.out.println("Token Kind:  " + decoded.tokenKind());
            System.out.println("Token ID:    " + decoded.tokenId());
            System.out.println("Issued At:   " + decoded.issuedAt());
            System.out.println("Expires At:  " + decoded.expiresAt());
            System.out.println("Authorities: " + decoded.authorities());

            System.out.println("=".repeat(60));
        } catch (Exception e) {
            System.out.println("解码失败: " + e.getMessage());
        }
    }

    /**
     * 解码后的 Token 内容
     */
    public record DecodedToken(
            Long userId,
            String username,
            Integer userType,
            List<String> authorities,
            String tokenKind,
            String tokenId,
            Date issuedAt,
            Date expiresAt
    ) {
    }

    // ========== 便捷 main 方法（直接在命令行运行） ==========
    public static void main(String[] args) {
        // TODO: 在这里替换成你的 JWT Token
        String YOUR_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMDIxNDg0ODAxODYwNTY3MDQyIiwiaXNzIjoibWVkaWFzayIsImlhdCI6MTc3MDc5NDUxMywiZXhwIjoxNzcxMzk5MzEzLCJ1c2VybmFtZSI6ImFkbWluIiwidXNlclR5cGUiOjEsInBlcm1zIjpbInNjaGVkdWxlOmNyZWF0ZSIsInNjaGVkdWxlOmF1dG8iLCJzY2hlZHVsZTp1cGRhdGUiXSwidG9rZW5LaW5kIjoiQUNDRVNTIiwianRpIjoiMGZjMzRiZWUtNTE1Yy00NmZjLTgyY2QtYTgxZjRjODI0MzNmIn0.35aM6fvsNxQYBwI9peigZSZ7TFrVCru4klJdfBpIV1Q";

        // JWT 签名密钥
        String SECRET = "mediask-dev-secret-please-change-32chars-min";

        System.out.println("=".repeat(60));
        System.out.println("JWT Token 解析结果");
        System.out.println("=".repeat(60));

        try {
            DecodedToken decoded = decode(YOUR_TOKEN, SECRET);

            System.out.println("User ID:     " + decoded.userId());
            System.out.println("Username:    " + decoded.username());
            System.out.println("User Type:   " + decoded.userType());
            System.out.println("Token Kind:  " + decoded.tokenKind());
            System.out.println("Token ID:    " + decoded.tokenId());
            System.out.println("Issued At:   " + decoded.issuedAt());
            System.out.println("Expires At:  " + decoded.expiresAt());
            System.out.println();
            System.out.println(">>> Authorities (权限列表): " + decoded.authorities());
            System.out.println();

            System.out.println("=".repeat(60));
        } catch (Exception e) {
            System.out.println("解码失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
