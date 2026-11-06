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
 * DeepSeek AI Provider 实现
 */
@Service("deepseekProvider")
public class DeepSeekProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekProvider.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api-url:}")
    private String apiUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    public DeepSeekProvider(RestTemplate restTemplate) {
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
            log.error("DeepSeek API call failed: {}", e.getMessage());
            throw new RuntimeException("AI接口调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() { return "DeepSeek"; }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("${DEEPSEEK_API_KEY}");
    }
}
