package com.divination.common.util;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.divination.service.impl.AiProviderManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * AI请求工具类 — 带熔断、限流、重试和异步支持。
 * 内部委托 {@link AiProviderManager} 实现多模型（DeepSeek / Qwen）主备切换，
 * 保留 Resilience4j 的熔断/限流/重试作为整体保护。
 */
@Component
public class AiRequestUtil {

    private static final Logger log = LoggerFactory.getLogger(AiRequestUtil.class);

    private final AiProviderManager providerManager;

    // Resilience4j components
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final RateLimiter rateLimiter;

    public AiRequestUtil(AiProviderManager providerManager) {
        this.providerManager = providerManager;

        // Circuit Breaker: 当失败率超过50%时打开，30秒后进入半开状态
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);
        this.circuitBreaker = cbRegistry.circuitBreaker("aiService");

        // Retry: 超时或网络异常时重试2次
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(
                        java.util.concurrent.TimeoutException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                .build();
        this.retry = Retry.of("aiService", retryConfig);

        // Time Limiter: 60秒超时
        this.timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(60))
                .cancelRunningFuture(true)
                .build());

        // Rate Limiter: 每分钟最多10个请求
        RateLimiterConfig rlConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .limitForPeriod(10)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
        this.rateLimiter = RateLimiter.of("aiService", rlConfig);
    }

    /**
     * 同步调用 AI（带熔断/重试/限流），内部经 AiProviderManager 做多模型主备切换。
     */
    public String callAi(String systemPrompt, String userPrompt) {
        log.debug("AI call started (provider-managed): system={}, user={}",
                systemPrompt != null ? systemPrompt.substring(0, Math.min(50, systemPrompt.length())) : "null",
                userPrompt != null ? userPrompt.substring(0, Math.min(50, userPrompt.length())) : "null");

        if (circuitBreaker.getState().equals(CircuitBreaker.State.OPEN)) {
            log.warn("Circuit breaker is OPEN for aiService — rejecting AI request");
            throw new RuntimeException("AI服务暂时不可用，请稍后再试");
        }

        rateLimiter.acquirePermission();

        try {
            Supplier<String> decorated = Retry.decorateSupplier(retry,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> providerManager.call(systemPrompt, userPrompt)));
            return decorated.get();
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("AI接口调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步调用 AI（非阻塞）。
     */
    @Async("aiCallExecutor")
    public CompletableFuture<String> callAiAsync(String systemPrompt, String userPrompt) {
        log.debug("AI async call scheduled");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callAi(systemPrompt, userPrompt);
            } catch (Exception e) {
                log.error("Async AI call failed: {}", e.getMessage());
                return "[AI服务暂时不可用] " + e.getMessage();
            }
        });
    }

    /**
     * 异步对话调用。
     */
    @Async("aiCallExecutor")
    public CompletableFuture<String> callAiChatAsync(String systemPrompt, List<Map<String, String>> messages) {
        log.debug("AI chat async call scheduled, history size={}",
                messages != null ? messages.size() : 0);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return callAiChat(systemPrompt, messages);
            } catch (Exception e) {
                log.error("Async AI chat call failed: {}", e.getMessage());
                return "[AI服务暂时不可用] " + e.getMessage();
            }
        });
    }

    /**
     * 同步对话调用 AI（带熔断/重试/限流），内部经 AiProviderManager 做多模型主备切换。
     */
    public String callAiChat(String systemPrompt, List<Map<String, String>> messages) {
        if (circuitBreaker.getState().equals(CircuitBreaker.State.OPEN)) {
            log.warn("Circuit breaker is OPEN for aiService — rejecting AI chat request");
            throw new RuntimeException("AI服务暂时不可用，请稍后再试");
        }

        rateLimiter.acquirePermission();

        try {
            Supplier<String> decorated = Retry.decorateSupplier(retry,
                    CircuitBreaker.decorateSupplier(circuitBreaker,
                            () -> providerManager.call(systemPrompt, messages)));
            return decorated.get();
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("AI接口调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * Get circuit breaker state for monitoring.
     */
    public Map<String, Object> getCircuitBreakerState() {
        var state = circuitBreaker.getState();
        var stats = circuitBreaker.getMetrics();
        return Map.of(
                "state", state.name().toLowerCase(),
                "failureRate", stats.getFailureRate(),
                "numberOfFailedCalls", stats.getNumberOfFailedCalls(),
                "numberOfSuccessfulCalls", stats.getNumberOfSuccessfulCalls(),
                "numberOfNotPermittedCalls", stats.getNumberOfNotPermittedCalls()
        );
    }
}
