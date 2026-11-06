package com.divination.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定服务
 * 连续失败5次后锁定15分钟，防止暴力破解
 */
@Service
public class LoginLockService {

    private static final Logger log = LoggerFactory.getLogger(LoginLockService.class);

    // 最大失败次数
    private static final int MAX_ATTEMPTS = 5;
    // 锁定时间（分钟）
    private static final long LOCK_DURATION_MINUTES = 15;

    // username -> login attempt info
    private final ConcurrentHashMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    /**
     * Check if the given username is currently locked out.
     * @return true if locked, false otherwise
     */
    public boolean isLocked(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null) return false;

        // Check if lock has expired
        if (LocalDateTime.now().isAfter(attempt.lockedAt.plusMinutes(LOCK_DURATION_MINUTES))) {
            // Lock expired, reset
            attempts.remove(username);
            log.info("Login lock expired for user: {}", username);
            return false;
        }

        return true;
    }

    /**
     * Record a failed login attempt. If max attempts reached, lock the account.
     * @return remaining lock time in minutes (0 if not locked yet, >0 if locked)
     */
    public int recordFailedAttempt(String username) {
        LocalDateTime now = LocalDateTime.now();
        LoginAttempt attempt = attempts.computeIfAbsent(username, k -> new LoginAttempt());
        attempt.lastFailedAt = now;
        attempt.failCount++;

        if (attempt.failCount >= MAX_ATTEMPTS) {
            attempt.lockedAt = now;
            log.warn("User {} locked out after {} failed attempts", username, attempt.failCount);
            return (int) LOCK_DURATION_MINUTES;
        }

        return 0;
    }

    /**
     * Reset failed attempts on successful login.
     */
    public void resetAttempts(String username) {
        attempts.remove(username);
    }

    /**
     * Get remaining lock time in minutes, or 0 if not locked.
     */
    public long getRemainingLockMinutes(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null || attempt.lockedAt == null) return 0;

        LocalDateTime unlockTime = attempt.lockedAt.plusMinutes(LOCK_DURATION_MINUTES);
        long remaining = java.time.Duration.between(LocalDateTime.now(), unlockTime).toMinutes();
        return Math.max(0, remaining + 1); // round up
    }

    private static class LoginAttempt {
        LocalDateTime lastFailedAt;
        int failCount = 0;
        LocalDateTime lockedAt;
    }
}
