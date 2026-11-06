package com.divination.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.divination.entity.IntegralShop;
import com.divination.entity.NotificationEntity;
import com.divination.entity.UserFeedback;

import java.util.Map;

public interface AdminService {
    Map<String, Object> getStatistics();
    Map<String, Object> getTrendData(int days);
    void disableUser(Long userId);
    void enableUser(Long userId);
    IPage<NotificationEntity> listNotifications(int page, int size, String type);
    void broadcastNotification(String title, String content, String type);
    IPage<UserFeedback> listFeedback(int page, int size, Integer status);
    void replyFeedback(Long id, String reply);
    void addShopItem(IntegralShop shopItem);
    void updateShopItem(IntegralShop shopItem);
}
