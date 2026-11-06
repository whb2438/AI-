package com.divination.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.divination.entity.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface UserService extends IService<User>, UserDetailsService {
    User register(String username, String password, String ip);
    User login(String username, String password, String ip);
    User getUserById(Long userId);
    IPage<User> listUsers(int page, int size, String username, String nickname, Integer status);
    void updateUser(User user);
    void deleteUser(Long userId);
    void resetPassword(Long userId, String newPassword);
    void adjustIntegral(Long userId, int amount);
    long countTodayUsers();

    // --- Sign-in ---
    SignInRecord signIn(Long userId);
    Map<String, Object> getSignInStatus(Long userId);

    // --- Fortune Calendar ---
    FortuneCalendar generateDailyFortune(Long userId);
    List<FortuneCalendar> getFortuneRange(Long userId, LocalDate start, LocalDate end);
    FortuneCalendar getFortuneByDate(Long userId, LocalDate date);

    // --- Push Settings ---
    PushNotification getPushSetting(Long userId);
    void togglePushSetting(Long userId, String settingKey, boolean enabled);

    // --- Favorites ---
    void createFavorite(Long userId, Long recordId);
    void removeFavorite(Long userId, Long recordId);
    boolean isFavorite(Long userId, Long recordId);
    List<DivinationRecord> getFavoriteRecords(Long userId);

    // --- Notifications ---
    void createNotification(Long userId, String title, String content, String type);
    void markNotificationRead(Long userId, Long notificationId);
    void markAllNotificationsRead(Long userId);
    int getUnreadCount(Long userId);
    IPage<NotificationEntity> getNotifications(Long userId, int page, int size);
    IPage<NotificationEntity> listAllNotifications(int page, int size, String type);
    void broadcastNotification(String title, String content, String type);

    // --- Feedback ---
    void submitFeedback(Long userId, String feedbackType, String content, String contact);
    IPage<UserFeedback> getUserFeedbackList(Long userId, int page, int size);
    IPage<UserFeedback> listAllFeedback(int page, int size, Integer status);
    void replyFeedback(Long id, String reply);

    // --- Shop ---
    List<IntegralShop> getShopItems(Integer enabled);
    Map<String, Object> redeemShopItem(Long userId, Long shopItemId);
    void addShopItem(IntegralShop shopItem);
    void updateShopItem(IntegralShop shopItem);

    // --- User Level ---
    Map<String, Object> getUserLevelData(Long userId);
    void updateLevelAfterDivination(Long userId, int costIntegral);

    // --- Divination Trend ---
    Map<String, Object> getDivinationTrend(Long userId, int days);

    // --- Backpack ---
    List<CardKey> getBackpack(Long userId);
    void useCardKey(Long userId, Long cardKeyId);

    // --- User Profile ---
    void updateUserProfile(Long userId, String realName, String gender,
                           String birthDate, String birthTime);
}
