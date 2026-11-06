package com.divination.service;

import java.util.List;
import java.util.Map;

/**
 * AI Provider 接口 — 抽象不同AI模型的调用方式
 * 支持切换 DeepSeek / Qwen / Kimi 等提供商
 */
public interface AiProvider {

    /**
     * 调用AI生成内容
     * @param systemPrompt 系统提示词
     * @param messages 对话历史消息
     * @return AI回复内容
     */
    String call(String systemPrompt, List<Map<String, String>> messages);

    /**
     * 获取提供商名称
     */
    String getName();

    /**
     * 获取当前配置是否可用
     */
    boolean isAvailable();

    /**
     * AI模型枚举
     */
    enum ModelType {
        DEEPSEEK_CHAT("deepseek-chat", "DeepSeek"),
        QWEN_TURBO("qwen-turbo", "通义千问Turbo"),
        QWEN_PLUS("qwen-plus", "通义千问Plus"),
        KIMI_CHAT("kimi-chat", "Moonshot Kimi");

        private final String apiModel;
        private final String displayName;

        ModelType(String apiModel, String displayName) {
            this.apiModel = apiModel;
            this.displayName = displayName;
        }

        public String getApiModel() { return apiModel; }
        public String getDisplayName() { return displayName; }
    }
}
