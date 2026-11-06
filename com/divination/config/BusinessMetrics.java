package com.divination.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 业务指标计数器 — 通过MeterRegistry注册自定义Prometheus指标
 */
@Component
public class BusinessMetrics {

    private final Counter aiCallsTotal;
    private final Counter aiCallsFailed;
    private final Counter divinationRecordsTotal;
    private final Counter signInsTotal;
    private final Counter paymentsTotal;

    public BusinessMetrics(MeterRegistry registry) {
        this.aiCallsTotal = Counter.builder("divination_ai_calls_total")
                .description("Total AI API calls")
                .register(registry);
        this.aiCallsFailed = Counter.builder("divination_ai_calls_failed")
                .description("Failed AI API calls")
                .register(registry);
        this.divinationRecordsTotal = Counter.builder("divination_records_total")
                .description("Total divination records created")
                .register(registry);
        this.signInsTotal = Counter.builder("divination_signins_total")
                .description("Total sign-in events")
                .register(registry);
        this.paymentsTotal = Counter.builder("divination_payments_total")
                .description("Total payment orders created")
                .register(registry);
    }

    public void recordAiCall(boolean success) {
        aiCallsTotal.increment();
        if (!success) aiCallsFailed.increment();
    }

    public void recordDivinationRecord() {
        divinationRecordsTotal.increment();
    }

    public void recordSignIn() {
        signInsTotal.increment();
    }

    public void recordPayment() {
        paymentsTotal.increment();
    }
}
