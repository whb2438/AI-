package com.divination.service;

import com.divination.entity.AiChatMessage;
import com.divination.entity.AiChatSession;
import java.util.List;
import java.util.Map;

public interface AiChatService {
    AiChatSession createSession(Long userId, Long recordId, String title);
    AiChatSession getSession(Long sessionId);
    List<AiChatMessage> getMessages(Long sessionId);
    AiChatMessage sendMessage(Long sessionId, Long userId, String role, String content);
    Map<String, Object> chatWithRecord(Long sessionId, Long userId, String message, String systemPrompt);
}
