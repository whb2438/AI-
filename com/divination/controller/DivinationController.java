package com.divination.controller;
import com.divination.common.result.Result;
import com.divination.common.util.AiRequestUtil;
import com.divination.config.AiConfig;
import com.divination.entity.DivinationRecord;
import com.divination.entity.AiChatSession;
import com.divination.entity.AiChatMessage;
import com.divination.entity.User;
import com.divination.mapper.UserMapper;
import com.divination.service.DivinationRecordService;
import com.divination.service.AiChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/divination")
public class DivinationController {
    private final DivinationRecordService divinationService;
    private final AiRequestUtil aiRequestUtil;
    private final AiConfig aiConfig;
    private final AiChatService aiChatService;
    private final UserMapper userMapper;
    private int onceCostIntegral;
    public DivinationController(DivinationRecordService divinationService, AiRequestUtil aiRequestUtil, AiConfig aiConfig, AiChatService aiChatService, UserMapper userMapper) {
        this.divinationService = divinationService;
        this.aiRequestUtil = aiRequestUtil;
        this.aiConfig = aiConfig;
        this.aiChatService = aiChatService;
        this.userMapper = userMapper;
    }
    @org.springframework.beans.factory.annotation.Value("${divination.once-cost-integral:10}")
    public void setOnceCostIntegral(int onceCostIntegral) {
        this.onceCostIntegral = onceCostIntegral;
    }
    @PostMapping("/create")
    public Result<DivinationRecord> create(@Valid @RequestBody CreateRequest request,
                                           HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        DivinationRecord record = divinationService.createDivination(
                request.getType(),
                request.getInputInfo(),
                onceCostIntegral,
                userId
        );
        return Result.success(record);
    }

    @GetMapping("/record/{id}")
    public Result<DivinationRecord> getRecordById(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        DivinationRecord record = divinationService.getRecord(id);
        if (record == null) {
            return Result.error(404, "记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            return Result.error(403, "无权访问他人记录");
        }
        return Result.success(record);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        DivinationRecord record = divinationService.getRecord(id);
        if (record == null) {
            return Result.error(404, "记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            return Result.error(403, "无权删除他人记录");
        }
        divinationService.deleteRecord(id);
        return Result.success();
    }

    // ===================== AI Chat Session Management =====================

    @PostMapping("/chat/session")
    public Result<AiChatSession> createChatSession(@Valid @RequestBody CreateChatSessionRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        Long recordId = null;
        try {
            recordId = Long.parseLong(request.getRecordId());
        } catch (NumberFormatException ignored) {}
        DivinationRecord record = recordId != null ? divinationService.getRecord(recordId) : null;
        String title = record != null ? record.getType() + "测算" : "AI追问";
        AiChatSession session = aiChatService.createSession(userId, recordId, title);
        return Result.success(session);
    }

    @GetMapping("/chat/session/{sessionId}")
    public Result<AiChatSession> getChatSession(@PathVariable Long sessionId,
                                                HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        AiChatSession session = aiChatService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error(404, "会话不存在");
        }
        return Result.success(session);
    }

    @GetMapping("/chat/session/{sessionId}/messages")
    public Result<List<AiChatMessage>> getChatMessages(@PathVariable Long sessionId,
                                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        AiChatSession session = aiChatService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error(404, "会话不存在");
        }
        List<AiChatMessage> messages = aiChatService.getMessages(sessionId);
        return Result.success(messages);
    }

    @PostMapping("/chat/session/{sessionId}/message")
    public Result<Map<String, Object>> sendChatMessage(@PathVariable Long sessionId,
                                                       @Valid @RequestBody SendChatMessageRequest request,
                                                       HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        AiChatSession session = aiChatService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error(404, "会话不存在");
        }
        Map<String, Object> data = aiChatService.chatWithRecord(
                sessionId, userId, request.getMessage(), aiConfig.getAiPrompt());
        return Result.success(data);
    }

    @PostMapping("/chat/session/standalone")
    public Result<AiChatSession> createStandaloneSession(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        AiChatSession session = aiChatService.createSession(userId, null, "AI自由对话");
        return Result.success(session);
    }

    // ===================== Legacy Chat (backward compat) =====================

    @PostMapping("/ai/chat")
    public Result<Map<String, String>> aiChat(@Valid @RequestBody AiChatRequest request,
                                               HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        // Build conversation: system prompt is handled by AiRequestUtil.callAiChat
        // Only pass the history + current user message
        List<Map<String, String>> messages = new ArrayList<>();
        String basePrompt = aiConfig.getAiPrompt() +
                "\n你是一个AI命理师，用户会给你一段之前的测算结果，并追问相关问题。请基于测算结果继续解答，保持古风典雅文风。";
        String enhancedPrompt = enhancePromptWithUserInfo(basePrompt, userId);
        if (request.getHistory() != null) {
            for (Map<String, String> msg : request.getHistory()) {
                // Normalize role: frontend uses "ai", DeepSeek expects "assistant"
                String role = "ai".equals(msg.get("role")) ? "assistant" : msg.get("role");
                messages.add(Map.of("role", role, "content", msg.get("content")));
            }
        }
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        try {
            // callAiChat will prepend the systemPrompt automatically
            String reply = aiRequestUtil.callAiChat(enhancedPrompt, messages);
            Map<String, String> data = new HashMap<>();
            data.put("reply", reply);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "AI服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 将用户个人信息（含生辰八字）注入到 systemPrompt 中
     */
    private String enhancePromptWithUserInfo(String systemPrompt, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getBirthDate() == null || user.getBirthDate().isEmpty()) {
            return systemPrompt;
        }
        String bazi = com.divination.common.util.DivinationBaziUtil.toBazi(user.getBirthDate(), user.getBirthTime());
        String name = user.getRealName() != null && !user.getRealName().isEmpty() ? user.getRealName() : "未填写";
        String gender = user.getGender() != null && !user.getGender().isEmpty() ? user.getGender() : "未填写";
        String userInfo = String.format("\n\n【用户个人信息】\n姓名：%s\n性别：%s\n公历生日：%s\n生辰八字：%s\n请基于以上用户信息提供个性化的命理解读。",
                name, gender, user.getBirthDate(), bazi);
        return systemPrompt + userInfo;
    }

    public static class AiChatRequest {
        @NotBlank(message = "消息内容不能为空")
        private String message;
        private List<Map<String, String>> history;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<Map<String, String>> getHistory() { return history; }
        public void setHistory(List<Map<String, String>> history) { this.history = history; }
    }

    public static class CreateChatSessionRequest {
        @NotBlank(message = "记录ID不能为空")
        private String recordId;
        public String getRecordId() { return recordId; }
        public void setRecordId(String recordId) { this.recordId = recordId; }
    }

    public static class SendChatMessageRequest {
        @NotBlank(message = "消息内容不能为空")
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class CreateRequest {
        @NotBlank(message = "测算类型不能为空")
        @Size(max = 50, message = "测算类型长度不能超过50")
        private String type;
        @NotBlank(message = "输入信息不能为空")
        @Size(max = 2000, message = "输入信息长度不能超过2000")
        private String inputInfo;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getInputInfo() { return inputInfo; }
        public void setInputInfo(String inputInfo) { this.inputInfo = inputInfo; }
    }
}
