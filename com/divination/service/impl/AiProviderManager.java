package com.divination.service.impl;

import com.divination.service.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Provider 编排器 —— 让已定义的 {@link AiProvider} 多模型抽象真正生效。
 *
 * <p>核心能力：
 * <ol>
 *   <li>按名称稳定排序（deepseek 优先，qwen 次之）依次尝试可用 Provider；</li>
 *   <li>某个 Provider 近期失败则进入 30s 冷却，避免对故障端点反复重试（轻量熔断）；</li>
 *   <li>全部不可用时抛出 RuntimeException，由上游业务决定降级策略。</li>
 * </ol>
 *
 * 注：部署时只需配置相应环境变量（DEEPSEEK_API_KEY / PAYMENT_QWEN_API_KEY）即可启用对应模型，
 * 未配置的 Provider 会被 {@link AiProvider#isAvailable()} 自动跳过。
 */
@Service
public class AiProviderManager {

    private static final Logger log = LoggerFactory.getLogger(AiProviderManager.class);

    /** 故障冷却时长：期间跳过该 Provider */
    private static final long COOLDOWN_MS = 30_000;

    private final List<AiProvider> providers;
    private final Map<String, Long> lastFailureAt = new ConcurrentHashMap<>();

    public AiProviderManager(List<AiProvider> providers) {
        this.providers = new ArrayList<>(providers);
        // 名称升序：deepseek < qwen，保证主模型优先
        this.providers.sort(Comparator.comparing(p -> p.getName() == null ? "" : p.getName().toLowerCase()));
        log.info("AI Provider 编排器初始化，已装载 Provider：{}",
                this.providers.stream().map(AiProvider::getName).toList());
    }

    /**
     * 主备切换核心：依次尝试可用且在冷却期外的 Provider，返回首个成功结果。
     */
    public String call(String systemPrompt, List<Map<String, String>> messages) {
        List<String> errors = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (AiProvider provider : providers) {
            if (!provider.isAvailable()) {
                continue;
            }
            Long failAt = lastFailureAt.get(provider.getName());
            if (failAt != null && now - failAt < COOLDOWN_MS) {
                continue; // 冷却中，跳过
            }
            try {
                String result = provider.call(systemPrompt, messages);
                if (result != null && !result.trim().isEmpty()) {
                    lastFailureAt.remove(provider.getName());
                    return result;
                }
                throw new RuntimeException("Provider 返回空内容");
            } catch (Exception e) {
                log.warn("AI Provider【{}】调用失败，尝试下一个：{}", provider.getName(), e.getMessage());
                lastFailureAt.put(provider.getName(), now);
                errors.add(provider.getName() + ": " + e.getMessage());
            }
        }

        throw new RuntimeException("所有 AI Provider 均不可用（" + String.join("; ", errors) + "）");
    }

    /**
     * 便捷方法：单条 userPrompt 场景（排盘/六爻/紫微等非对话调用）。
     */
    public String call(String systemPrompt, String userPrompt) {
        return call(systemPrompt, List.of(Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));
    }
}
