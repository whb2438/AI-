package com.divination.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 推送服务 — 通过 STOMP 协议向客户端实时推送消息
 */
@Service
public class WebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 向指定用户推送通知消息
     * @param userId 目标用户ID
     * @param title 标题
     * @param content 内容
     * @param type 类型（SYSTEM / PUSH / PROMOTION）
     */
    public void sendNotification(Long userId, String title, String content, String type) {
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("id", System.currentTimeMillis());
            payload.put("title", title);
            payload.put("content", content);
            payload.put("type", type);
            payload.put("isRead", false);
            payload.put("createTime", java.time.LocalDateTime.now().toString());

            // 发送到 /topic/notification/{userId}
            messagingTemplate.convertAndSend(
                    "/topic/notification/" + userId, payload);
        } catch (Exception e) {
            // 静默失败，不影响主流程
        }
    }

    /**
     * 广播通知给所有在线用户
     */
    public void broadcastAll(String title, String content, String type) {
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("id", System.currentTimeMillis());
            payload.put("title", title);
            payload.put("content", content);
            payload.put("type", type);
            payload.put("isRead", false);
            payload.put("createTime", java.time.LocalDateTime.now().toString());

            messagingTemplate.convertAndSend("/topic/broadcast", payload);
        } catch (Exception e) {
            // 静默失败
        }
    }

    /**
     * 推送运势更新
     */
    public void sendFortuneUpdate(Long userId, int overallScore) {
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("type", "FORTUNE_UPDATE");
            payload.put("overallScore", overallScore);
            payload.put("date", java.time.LocalDate.now().toString());

            messagingTemplate.convertAndSend(
                    "/topic/fortune/" + userId, payload);
        } catch (Exception e) {
            // 静默失败
        }
    }

    /**
     * 推送AI测算结果就绪
     */
    public void sendDivinationReady(Long userId, Long recordId, String resultPreview) {
        try {
            var payload = new java.util.HashMap<String, Object>();
            payload.put("type", "DIVINATION_READY");
            payload.put("recordId", recordId);
            payload.put("preview", resultPreview != null ? resultPreview.substring(0, Math.min(100, resultPreview.length())) : "");

            messagingTemplate.convertAndSend(
                    "/topic/divination/" + userId, payload);
        } catch (Exception e) {
            // 静默失败
        }
    }
}
