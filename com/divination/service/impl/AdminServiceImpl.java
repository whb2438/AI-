package com.divination.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.divination.common.exception.BusinessException;
import com.divination.common.result.ResultCode;
import com.divination.entity.DivinationRecord;
import com.divination.entity.IntegralShop;
import com.divination.entity.NotificationEntity;
import com.divination.entity.User;
import com.divination.entity.UserFeedback;
import com.divination.mapper.RecordMapper;
import com.divination.service.AdminService;
import com.divination.service.UserService;
import com.divination.service.CardService;
import com.divination.service.DivinationRecordService;
import com.divination.mapper.SignInRecordMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class AdminServiceImpl implements AdminService {
    private final UserService userService;
    private final CardService cardService;
    private final DivinationRecordService divinationService;
    private final RecordMapper recordMapper;
    private final SignInRecordMapper signInRecordMapper;
    public AdminServiceImpl(UserService userService, CardService cardService, DivinationRecordService divinationService, RecordMapper recordMapper, SignInRecordMapper signInRecordMapper) {
        this.userService = userService;
        this.cardService = cardService;
        this.divinationService = divinationService;
        this.recordMapper = recordMapper;
        this.signInRecordMapper = signInRecordMapper;
    }
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.count());
        stats.put("todayNewUsers", userService.countTodayUsers());
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LambdaQueryWrapper<DivinationRecord> divWrapper = new LambdaQueryWrapper<>();
        divWrapper.ge(DivinationRecord::getCreateTime, startOfDay);
        long todayDivCount = divinationService.count(divWrapper);
        stats.put("todayDivinationCount", todayDivCount);
        long totalIntegralConsumed = recordMapper.sumCostIntegral();
        stats.put("totalIntegralConsumed", totalIntegralConsumed);
        stats.put("todayCardExchanges", cardService.countTodayExchanges());
        return stats;
    }

    @Override
    public Map<String, Object> getTrendData(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        LocalDateTime startDt = startDate.atStartOfDay();
        LocalDateTime endDt = endDate.plusDays(1).atStartOfDay();

        // 1. Divination trend by date
        List<DivinationRecord> divRecords = divinationService.list(
                new LambdaQueryWrapper<DivinationRecord>()
                        .ge(DivinationRecord::getCreateTime, startDt)
                        .le(DivinationRecord::getCreateTime, endDt));
        Map<String, Long> divByDate = new HashMap<>();
        Map<String, Map<String, Long>> divByTypeAndDate = new HashMap<>();
        for (DivinationRecord r : divRecords) {
            String dateStr = r.getCreateTime().toLocalDate().toString();
            divByDate.merge(dateStr, 1L, Long::sum);
            String type = r.getType();
            divByTypeAndDate.computeIfAbsent(type, k -> new HashMap<>())
                    .merge(dateStr, 1L, Long::sum);
        }
        // Build divinationTrend: {date, value} for every day in range
        List<Map<String, Object>> divTrend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String d = startDate.plusDays(i).toString();
            divTrend.add(Map.of("date", d, "value", divByDate.getOrDefault(d, 0L)));
        }
        result.put("divinationTrend", divTrend);

        // Build divinationByType: type-name -> [{date, count}]
        Map<String, List<Map<String, Object>>> typeBreakdown = new HashMap<>();
        for (Map.Entry<String, Map<String, Long>> entry : divByTypeAndDate.entrySet()) {
            String typeName = entry.getKey();
            List<Map<String, Object>> typeData = new ArrayList<>();
            Map<String, Long> typeCounts = entry.getValue();
            for (int i = 0; i < days; i++) {
                String d = startDate.plusDays(i).toString();
                typeData.add(Map.of("date", d, "count", typeCounts.getOrDefault(d, 0L)));
            }
            typeBreakdown.put(typeName, typeData);
        }
        result.put("divinationByType", typeBreakdown);

        // 2. Sign-in trend by date
        List<com.divination.entity.SignInRecord> signInRecords = signInRecordMapper.selectList(
                new LambdaQueryWrapper<com.divination.entity.SignInRecord>()
                        .ge(com.divination.entity.SignInRecord::getSignInDate, startDate)
                        .le(com.divination.entity.SignInRecord::getSignInDate, endDate));
        Map<String, Long> signByDate = new HashMap<>();
        for (com.divination.entity.SignInRecord s : signInRecords) {
            String d = s.getSignInDate().toString();
            signByDate.merge(d, 1L, Long::sum);
        }
        List<Map<String, Object>> signTrend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String d = startDate.plusDays(i).toString();
            signTrend.add(Map.of("date", d, "value", signByDate.getOrDefault(d, 0L)));
        }
        result.put("signTrend", signTrend);

        return result;
    }

    @Override
    public void disableUser(Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            int nextVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 1) + 1;
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            wrapper.eq(com.divination.entity.User::getId, userId)
                   .set(com.divination.entity.User::getStatus, 0)
                   .set(com.divination.entity.User::getTokenVersion, nextVersion);
            userService.update(wrapper);
        }
    }
    @Override
    public void enableUser(Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            int nextVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 1) + 1;
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            wrapper.eq(com.divination.entity.User::getId, userId)
                   .set(com.divination.entity.User::getStatus, 1)
                   .set(com.divination.entity.User::getTokenVersion, nextVersion);
            userService.update(wrapper);
        }
    }
    @Override
    public IPage<NotificationEntity> listNotifications(int page, int size, String type) {
        return userService.listAllNotifications(page, size, type);
    }
    @Override
    public void broadcastNotification(String title, String content, String type) {
        userService.broadcastNotification(title, content, type);
    }
    @Override
    public IPage<UserFeedback> listFeedback(int page, int size, Integer status) {
        return userService.listAllFeedback(page, size, status);
    }
    @Override
    public void replyFeedback(Long id, String reply) {
        userService.replyFeedback(id, reply);
    }
    @Override
    public void addShopItem(IntegralShop shopItem) {
        userService.addShopItem(shopItem);
    }
    @Override
    public void updateShopItem(IntegralShop shopItem) {
        userService.updateShopItem(shopItem);
    }
}
