package com.divination.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.model}")
    private String model;

    @Value("${divination.ai-prompt}")
    private String aiPrompt;

    public String getApiUrl() { return apiUrl; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
    public String getAiPrompt() { return aiPrompt; }
}
