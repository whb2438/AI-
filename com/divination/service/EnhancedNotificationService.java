package com.divination.service;

import com.divination.entity.NotificationEntity;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.time.LocalDateTime;

/**
 * 通知服务 — 整合数据库持久化 + WebSocket实时推送
 */
public interface EnhancedNotificationService {

    /**
     * 创建通知并实时推送（替代原有的 UserService.createNotification）
     */
    void createAndPush(Long userId, String title, String content, String type);

    /**
     * 广播通知给所有用户
     */
    void broadcastAll(String title, String content, String type);

    /**
     * 获取用户通知列表
     */
    IPage<NotificationEntity> getNotifications(Long userId, int page, int size);

    /**
     * 标记已读
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 全部已读
     */
    void markAllRead(Long userId);

    /**
     * 未读数
     */
    int getUnreadCount(Long userId);
}
