package com.divination.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * 自定义指标 — 业务监控（AI调用次数、用户活跃等）
 */
@Component
public class CustomMetricsConfig {

    public CustomMetricsConfig(MeterRegistry registry) {
        // AI调用计数器
        registry.counter("divination.ai.calls.total").increment();
        // 注册AI调用计数器
        var aiCalls = io.micrometer.core.instrument.Counter.builder("divination.ai.calls.total")
                .tag("type", "total")
                .description("Total AI API calls made")
                .register(registry);
        // 注册AI调用失败计数器
        io.micrometer.core.instrument.Counter.builder("divination.ai.calls.failed")
                .tag("type", "total")
                .description("Failed AI API calls")
                .register(registry);
        // 注册测算次数计数器
        io.micrometer.core.instrument.Counter.builder("divination.records.total")
                .tag("type", "total")
                .description("Total divination records created")
                .register(registry);
    }
}
