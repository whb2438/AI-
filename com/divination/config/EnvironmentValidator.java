package com.divination.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Spring Boot application listener that validates required environment variables
 * before the application context fully initializes.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvironmentValidator implements ApplicationListener<ApplicationFailedEvent> {
    private static final Logger log = LoggerFactory.getLogger(EnvironmentValidator.class);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        // Only log — actual validation happens in DivinationApplication.validateEnvironment()
        Throwable ex = event.getException();
        if (ex != null && ex instanceof IllegalStateException) {
            log.error("❌ Application failed to start: {}", ex.getMessage());
            log.error("Please set the required environment variables:");
            log.error("  DEEPSEEK_API_KEY=<your-deepseek-api-key>");
            log.error("  JWT_SECRET=<at-least-32-characters-long-secret-key>");
            log.error("  DB_PASSWORD=<your-database-password>");
        }
    }
}
