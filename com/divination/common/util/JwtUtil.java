package com.divination.common.util;

import com.divination.common.constant.JwtConstants;
import com.divination.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - 安全增强版
 * 包含密钥校验、Token版本管理、刷新等功能
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expire-time:86400}") // 默认24小时，单位秒
    private long expireTime;

    /**
     * 初始化时验证密钥安全性
     * 注意：@Value 字段注入发生在构造之后，校验必须放在 @PostConstruct 中，
     * 否则 secret 永远为 null，应用必然启动失败。
     */
    @jakarta.annotation.PostConstruct
    public void validateSecret() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret not configured! Set jwt.secret in application properties");
        }
        if (secret.length() < 32) {
            log.warn("JWT secret should be at least 32 characters for HS256 security");
        }
    }

    /**
     * Generate JWT token for given user with enhanced claims.
     */
    public String generateToken(Long userId, String username, String role, Integer tokenVersion) {
        return generateToken(userId, username, role, tokenVersion, expireTime);
    }

    /**
     * Generate JWT token with custom expiration time.
     */
    public String generateToken(Long userId, String username, String role, Integer tokenVersion, long expireSeconds) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIM_USER_ID, userId);
        claims.put(JwtConstants.CLAIM_USERNAME, username);
        claims.put(JwtConstants.CLAIM_ROLE, role);
        claims.put(JwtConstants.CLAIM_TOKEN_VERSION, tokenVersion);
        claims.put(JwtConstants.CLAIM_ISSUED_AT, System.currentTimeMillis());
        claims.put(JwtConstants.CLAIM_ISSUER, "divination-platform");

        Date now = new Date();
        Date expire = new Date(now.getTime() + expireSeconds * 1000L);
        SecretKey key = getSecurityKey();

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key)
                .compact();
    }

    /**
     * Parse and validate JWT token, return claims.
     * Supports token version checking via isTokenVersionValid method.
     */
    public Claims parseToken(String token) {
        try {
            SecretKey key = getSecurityKey();
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Invalid JWT token parsing: {}", e.getMessage());
            throw new BusinessException(401, "Token无效或已过期");
        }
    }

    /**
     * Extract userId from token with null-safe handling.
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(JwtConstants.CLAIM_USER_ID, Long.class);
    }

    /**
     * Extract username from token with null-safe handling.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(JwtConstants.CLAIM_USERNAME, String.class);
    }

    /**
     * Extract role from token with null-safe handling.
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(JwtConstants.CLAIM_ROLE, String.class);
    }

    /**
     * Check if token is expired using standard parsing.
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true; // Invalid token considered expired
        }
    }

    /**
     * Extract tokenVersion from token. Returns null for pre-migration tokens.
     */
    public Integer getTokenVersionFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get(JwtConstants.CLAIM_TOKEN_VERSION, Integer.class);
    }

    /**
     * Validate that the token's version matches the expected DB version.
     * Tokens without a version claim are accepted during transition period.
     */
    public boolean isTokenVersionValid(String token, Integer expectedVersion) {
        try {
            Integer tokenVersion = getTokenVersionFromToken(token);
            if (tokenVersion == null) {
                return true; // transition: accept pre-migration tokens
            }
            return tokenVersion.equals(expectedVersion);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get remaining validity time of token in seconds.
     */
    public long getTokenRemainingSeconds(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long seconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, seconds);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Extract issued timestamp from token.
     */
    public long getIssuedAt(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get(JwtConstants.CLAIM_ISSUED_AT, Long.class);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Build a security key from the configured secret.
     */
    private SecretKey getSecurityKey() {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        // Ensure key is at least 32 bytes for HS256
        if (secretBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(secretBytes, 0, paddedKey, 0, secretBytes.length);
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }
}
