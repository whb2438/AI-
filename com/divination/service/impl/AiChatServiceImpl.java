package com.divination.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.divination.common.util.AiRequestUtil;
import com.divination.common.util.DivinationBaziUtil;
import com.divination.entity.AiChatMessage;
import com.divination.entity.AiChatSession;
import com.divination.entity.DivinationRecord;
import com.divination.entity.User;
import com.divination.mapper.AiChatMessageMapper;
import com.divination.mapper.AiChatSessionMapper;
import com.divination.mapper.UserMapper;
import com.divination.service.AiChatService;
import com.divination.service.DivinationRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatServiceImpl implements AiChatService {
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final AiRequestUtil aiRequestUtil;
    private final DivinationRecordService divinationRecordService;

    public AiChatServiceImpl(AiChatSessionMapper sessionMapper,
                             AiChatMessageMapper messageMapper,
                             UserMapper userMapper,
                             AiRequestUtil aiRequestUtil,
                             DivinationRecordService divinationRecordService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.aiRequestUtil = aiRequestUtil;
        this.divinationRecordService = divinationRecordService;
    }

    @Override
    @Transactional
    public AiChatSession createSession(Long userId, Long recordId, String title) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setRecordId(recordId);
        session.setTitle(title);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public AiChatSession getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    public List<AiChatMessage> getMessages(Long sessionId) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessage::getSessionId, sessionId)
               .orderByAsc(AiChatMessage::getCreateTime);
        return messageMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public AiChatMessage sendMessage(Long sessionId, Long userId, String role, String content) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权限");
        }
        AiChatMessage msg = new AiChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
        // Update session time
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        return msg;
    }

    @Override
    @Transactional
    public Map<String, Object> chatWithRecord(Long sessionId, Long userId, String message, String systemPrompt) {
        AiChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权限");
        }

        // Save user message
        sendMessage(sessionId, userId, "user", message);

        // Fetch chat history
        List<AiChatMessage> messages = getMessages(sessionId);

        // Build conversation history for AI
        List<Map<String, String>> history = new ArrayList<>();

        // If session is linked to a divination record, inject the original result as context
        if (session.getRecordId() != null) {
            DivinationRecord record = divinationRecordService.getRecord(session.getRecordId());
            if (record != null && record.getResult() != null && !record.getResult().isEmpty()) {
                history.add(Map.of("role", "assistant", "content",
                        "【原始测算结果】\n" + record.getResult()));
            }
        }

        for (AiChatMessage m : messages) {
            // DeepSeek API uses "assistant" not "ai" for role
            String role = "ai".equals(m.getRole()) ? "assistant" : m.getRole();
            history.add(Map.of("role", role, "content", m.getContent()));
        }

        // Call AI with history — inject user bazi info into systemPrompt
        String reply;
        try {
            String enhancedPrompt = enhancePromptWithUserInfo(systemPrompt, session.getUserId());
            reply = aiRequestUtil.callAiChat(enhancedPrompt, history);
        } catch (Exception e) {
            reply = "AI服务暂时不可用，请稍后重试。";
        }

        // Save AI reply
        AiChatMessage aiMsg = sendMessage(sessionId, userId, "assistant", reply);

        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        data.put("messageId", aiMsg.getId());
        return data;
    }

    /**
     * 将用户个人信息（含生辰八字）注入到 systemPrompt 中
     */
    private String enhancePromptWithUserInfo(String systemPrompt, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getBirthDate() == null || user.getBirthDate().isEmpty()) {
            return systemPrompt;
        }
        String bazi = DivinationBaziUtil.toBazi(user.getBirthDate(), user.getBirthTime());
        String name = user.getRealName() != null && !user.getRealName().isEmpty() ? user.getRealName() : "未填写";
        String gender = user.getGender() != null && !user.getGender().isEmpty() ? user.getGender() : "未填写";
        String userInfo = String.format("\n\n【用户个人信息】\n姓名：%s\n性别：%s\n公历生日：%s\n生辰八字：%s\n请基于以上用户信息提供个性化的命理解读。",
                name, gender, user.getBirthDate(), bazi);
        return systemPrompt + userInfo;
    }
}
