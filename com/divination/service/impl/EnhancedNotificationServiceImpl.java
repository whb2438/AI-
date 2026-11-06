package com.divination.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.divination.entity.NotificationEntity;
import com.divination.mapper.NotificationMapper;
import com.divination.service.EnhancedNotificationService;
import com.divination.service.WebSocketPushService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnhancedNotificationServiceImpl implements EnhancedNotificationService {

    private final NotificationMapper notificationMapper;
    private final WebSocketPushService pushService;

    public EnhancedNotificationServiceImpl(NotificationMapper notificationMapper,
                                           WebSocketPushService pushService) {
        this.notificationMapper = notificationMapper;
        this.pushService = pushService;
    }

    @Override
    @Transactional
    public void createAndPush(Long userId, String title, String content, String type) {
        // 1. 持久化到数据库
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notificationMapper.insert(notification);

        // 2. 通过WebSocket实时推送
        pushService.sendNotification(userId, title, content, type);
    }

    @Override
    @Transactional
    public void broadcastAll(String title, String content, String type) {
        // 获取所有活跃用户ID
        LambdaQueryWrapper<com.divination.entity.User> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(com.divination.entity.User::getId)
                .eq(com.divination.entity.User::getStatus, 1);
        var users = notificationMapper.selectList(new LambdaQueryWrapper<>())
                .stream()
                .map(n -> n.getUserId())
                .distinct()
                .toList();

        // 为每个用户创建通知并推送
        for (Long userId : users) {
            createAndPush(userId, title, content, type);
        }

        // 同时广播给所有WebSocket连接（已在线的用户）
        pushService.broadcastAll(title, content, type);
    }

    @Override
    public IPage<NotificationEntity> getNotifications(Long userId, int page, int size) {
        Page<NotificationEntity> pageParam = new Page<>(page, size);
        return notificationMapper.selectPage(pageParam,
                new LambdaQueryWrapper<NotificationEntity>()
                        .eq(NotificationEntity::getUserId, userId)
                        .orderByDesc(NotificationEntity::getCreateTime));
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationEntity>()
                .eq(NotificationEntity::getId, notificationId)
                .eq(NotificationEntity::getUserId, userId)
                .set(NotificationEntity::getIsRead, 1));
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationEntity>()
                .eq(NotificationEntity::getUserId, userId)
                .set(NotificationEntity::getIsRead, 1));
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }
}
