package com.divination;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 主应用类 - 启用异步、定时任务和缓存支持
 */
@SpringBootApplication
@MapperScan("com.divination.mapper")
@EnableAsync
@EnableScheduling
@EnableCaching // 添加缓存支持
public class DivinationApplication {
    private static final Logger log = LoggerFactory.getLogger(DivinationApplication.class);

    @Value("${deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    public static void main(String[] args) {
        SpringApplication.run(DivinationApplication.class, args);
    }

    @PostConstruct
    public void validateEnvironment() {
        // JWT secret: 安全刚需，缺了不能启动（签名所有 Token）
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.error("❌ FATAL: JWT_SECRET is not configured! Application cannot start without a secure JWT signing key.");
            throw new IllegalStateException("JWT_SECRET must be configured (minimum 32 characters recommended)");
        }
        if (jwtSecret.length() < 32) {
            log.warn("⚠️  WARNING: JWT_SECRET is only {} characters — recommend at least 32 characters for security", jwtSecret.length());
        }

        // DeepSeek API Key: 业务依赖，缺失时仅 warn（AI 解读相关接口会报错，其他功能正常）
        if (deepseekApiKey == null || deepseekApiKey.isEmpty()) {
            log.warn("⚠️  DEEPSEEK_API_KEY is not set! AI 解读相关接口将无法使用，其他功能不受影响。" +
                     "请通过环境变量 DEEPSEEK_API_KEY 或 application.yml deepseek.api-key 配置。");
        } else if (!deepseekApiKey.startsWith("sk-")) {
            log.warn("⚠️  WARNING: DEEPSEEK_API_KEY does not start with 'sk-' — verify it is correct");
        }

        log.info("✅ Environment validation completed");
    }
}
