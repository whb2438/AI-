package com.divination.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.divination.service.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 通义千问(Qwen) AI Provider 实现
 */
@Service("qwenProvider")
public class QwenProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(QwenProvider.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${payment.qwen.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String apiUrl;

    @Value("${payment.qwen.api-key:}")
    private String apiKey;

    @Value("${payment.qwen.model:qwen-turbo}")
    private String model;

    public QwenProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String call(String systemPrompt, List<Map<String, String>> messages) {
        var fullMessages = new java.util.ArrayList<Map<String, String>>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            fullMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (Map<String, String> msg : messages) {
            String role = "ai".equals(msg.get("role")) ? "assistant" : msg.get("role");
            fullMessages.add(Map.of("role", role, "content", msg.get("content")));
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", fullMessages,
                "temperature", 0.7,
                "max_tokens", 2000
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            throw new RuntimeException("AI返回格式异常");
        } catch (Exception e) {
            log.error("Qwen API call failed: {}", e.getMessage());
            throw new RuntimeException("AI接口调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() { return "Qwen"; }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${");
    }
}
