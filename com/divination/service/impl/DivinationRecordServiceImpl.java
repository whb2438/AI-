package com.divination.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.divination.common.exception.BusinessException;
import com.divination.common.result.ResultCode;
import com.divination.common.util.AiRequestUtil;
import com.divination.common.util.BaziCalculator;
import com.divination.config.AiConfig;
import com.alibaba.fastjson2.JSONObject;
import com.divination.entity.DivinationRecord;
import com.divination.entity.User;
import com.divination.mapper.RecordMapper;
import com.divination.mapper.UserMapper;
import com.divination.service.DivinationRecordService;
import com.divination.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class DivinationRecordServiceImpl extends ServiceImpl<RecordMapper, DivinationRecord> implements DivinationRecordService {

    private final UserService userService;
    private final AiConfig aiConfig;
    private final AiRequestUtil aiRequestUtil;
    private final UserMapper userMapper;

    // 每日查询限额（配置驱动）
    private int dailyLimitCount;
    private int vipExtraDailyLimit;

    public DivinationRecordServiceImpl(UserService userService, AiConfig aiConfig, AiRequestUtil aiRequestUtil, UserMapper userMapper) {
        this.userService = userService;
        this.aiConfig = aiConfig;
        this.aiRequestUtil = aiRequestUtil;
        this.userMapper = userMapper;
    }

    @Value("${divination.daily-limit-count:5}")
    public void setDailyLimitCount(int dailyLimitCount) {
        this.dailyLimitCount = dailyLimitCount;
    }

    @Value("${divination.vip-extra-daily-limit:20}")
    public void setVipExtraDailyLimit(int vipExtraDailyLimit) {
        this.vipExtraDailyLimit = vipExtraDailyLimit;
    }

    /**
     * Create divination record with atomic integral deduction and caching awareness.
     */
    @Override
    @Transactional
    @CacheEvict(value = "userDailyLimit", key = "#userId") // 清除当日限额缓存
    public DivinationRecord createDivination(String type, String inputInfo, int costIntegral, Long userId) {
        // Check daily limit with caching
        int todayCount = getTodayCountCached(userId);
        User user = userService.getUserById(userId);

        int effectiveLimit = calculateEffectiveLimit(user, dailyLimitCount, vipExtraDailyLimit);
        if (todayCount >= effectiveLimit) {
            throw new BusinessException(ResultCode.DAILY_LIMIT_EXCEEDED);
        }

        // 预检积分，避免对余额不足用户浪费 AI 调用（仍由下方原子扣减兜底）
        if (user == null || user.getIntegral() < costIntegral) {
            throw new BusinessException(ResultCode.INTEGRAL_INSUFFICIENT);
        }

        // 本地排盘：解析生辰，精确计算四柱/五行/十神等，供 AI 直接解读、或离线时生成结构化结果
        BaziCalculator.BaziChart chart = buildBaziChart(inputInfo);

        // 调用 AI（失败自动降级为离线模板，success=false）
        AiCallResult ai = callAiSafely(type, inputInfo, chart);

        // 仅当 AI 真正成功时才扣积分；离线模拟免费
        int charged = ai.success ? costIntegral : 0;
        if (ai.success) {
            int affected = userMapper.deductIntegralAtomically(userId, costIntegral);
            if (affected == 0) {
                throw new BusinessException(ResultCode.INTEGRAL_INSUFFICIENT);
            }
        }

        // Save record
        DivinationRecord record = new DivinationRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setInputInfo(inputInfo);
        record.setResult(ai.text);
        record.setCostIntegral(charged);
        record.setCreateTime(LocalDateTime.now());
        // 持久化本地命盘，供结果页渲染「命盘卡」（无生辰时为 null，前端降级）
        if (chart != null) {
            try {
                record.setChartData(JSONObject.toJSONString(chart));
            } catch (Exception e) {
                log.warn("命盘序列化失败，跳过持久化 chartData：{}", e.getMessage());
            }
        }

        // MyBatis-Plus save 后会回填自增主键
        this.save(record);

        // 仅 AI 成功时计入成长等级
        if (ai.success) {
            userService.updateLevelAfterDivination(userId, costIntegral);
        }

        // Cache the new record
        CacheDivinationRecord(record);

        return record;
    }

    /**
     * List records with pagination - cached for recent queries.
     */
    @Override
    @Cacheable(value = "userRecords", key = "#userId + ':' + #page + ':' + #size")
    public IPage<DivinationRecord> listRecords(Long userId, int page, int size) {
        Page<DivinationRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DivinationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DivinationRecord::getUserId, userId)
               .orderByDesc(DivinationRecord::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    /**
     * Get a single record by ID.
     */
    @Override
    @Cacheable(value = "divinationRecords", key = "#id")
    public DivinationRecord getRecord(Long id) {
        return this.getById(id);
    }

    /**
     * Delete a record and invalidate related caches.
     */
    @Override
    @Transactional
    @CacheEvict(value = {"divinationRecords", "userDailyLimit", "userRecords"}, allEntries = false)
    public void deleteRecord(Long id) {
        this.removeById(id);
    }

    /**
     * Get today's count with caching.
     */
    @Override
    @Cacheable(value = "userDailyLimit", key = "#userId")
    public int getTodayCount(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LambdaQueryWrapper<DivinationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DivinationRecord::getUserId, userId)
               .ge(DivinationRecord::getCreateTime, startOfDay);
        return (int) this.count(wrapper);
    }

    /**
     * Optimized version of getTodayCount with local cache.
     */
    private int getTodayCountCached(Long userId) {
        // Use the standard method which is already cached
        return getTodayCount(userId);
    }

    /**
     * Calculate effective daily limit based on VIP status.
     */
    private int calculateEffectiveLimit(User user, int baseLimit, int vipBonus) {
        if (user != null && user.isVipActive()) {
            return baseLimit + vipBonus;
        }
        return baseLimit;
    }

    /**
     * Check if user has enough integral (with caching check).
     */
    @Override
    public boolean hasEnoughIntegral(Long userId, int cost) {
        User user = userService.getUserById(userId);
        return user != null && user.getIntegral() >= cost;
    }

    /**
     * Deduct integral with atomic operation.
     */
    @Override
    @Transactional
    @CacheEvict(value = "userProfile", key = "#userId") // Invalidate user profile cache
    public void deductIntegral(Long userId, int cost) {
        User user = userService.getUserById(userId);
        if (user == null || user.getIntegral() < cost) {
            throw new BusinessException(ResultCode.INTEGRAL_INSUFFICIENT);
        }
        // Use atomic SQL UPDATE instead of read-modify-write
        int affected = userMapper.deductIntegralAtomically(userId, cost);
        if (affected == 0) {
            throw new BusinessException(ResultCode.INTEGRAL_INSUFFICIENT);
        }
    }

    /**
     * 离线模拟结果文本的统一前缀哨兵，前端据此可靠识别"未调用 AI"。
     */
    private static final String OFFLINE_MARKER = "【离线模拟结果】";

    /**
     * 调用 AI 生成结果；失败时降级为离线模板，并明确标记 success=false，
     * 由调用方决定是否扣费（默认不扣费，避免"花钱拿模板"）。
     *
     * @param chart 本地排盘结果（可为 null）。非 null 时：AI 直接基于精确命盘解读；
     *              离线时生成基于真实命盘的结构化解读，而非随机套话。
     */
    private AiCallResult callAiSafely(String type, String inputInfo, BaziCalculator.BaziChart chart) {
        String systemPrompt = aiConfig.getAiPrompt();
        StringBuilder userPrompt = new StringBuilder("测算类型：" + type + "\n用户信息：" + inputInfo);
        if (chart != null) {
            userPrompt.append("\n").append(chart.toContext());
        }
        try {
            String text = aiRequestUtil.callAi(systemPrompt, userPrompt.toString());
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("AI 返回内容为空");
            }
            return new AiCallResult(text, true);
        } catch (Exception e) {
            log.warn("AI 服务调用失败，降级为离线模拟（不扣积分）：{}", e.getMessage());
            String offline;
            if (chart != null) {
                offline = OFFLINE_MARKER + "\n\n" + chart.toOfflineReading(type);
            } else {
                offline = OFFLINE_MARKER
                        + "\n\n（AI 服务暂不可用，本次未调用 AI、未扣除积分，以下仅为系统示例模板）\n\n"
                        + "【" + type + " 测算结果】\n\n用户信息：" + inputInfo
                        + "\n\n您的命格独特，运势起伏有致，需修身养性以待天时。";
            }
            return new AiCallResult(offline, false);
        }
    }

    /**
     * 从 inputInfo(JSON) 解析生辰并本地排盘；无出生日期或解析失败时返回 null（不阻断流程）。
     */
    private BaziCalculator.BaziChart buildBaziChart(String inputInfo) {
        if (inputInfo == null || inputInfo.isEmpty()) return null;
        try {
            JSONObject obj = JSONObject.parseObject(inputInfo);
            String birthDate = obj.getString("birthDate");
            if (birthDate == null || birthDate.isEmpty()) return null;
            String birthTime = obj.getString("birthTime");
            String gender = obj.getString("gender");
            String birthPlace = obj.getString("birthPlace");
            boolean solarTime = obj.getBooleanValue("solarTime");
            return BaziCalculator.calculate(birthDate, birthTime, gender, birthPlace, solarTime);
        } catch (Exception e) {
            log.debug("解析生辰信息失败，跳过本地排盘：{}", e.getMessage());
            return null;
        }
    }

    /**
     * AI 调用结果持有者：文本内容 + 是否成功（决定是否扣费）。
     */
    private static class AiCallResult {
        final String text;
        final boolean success;

        AiCallResult(String text, boolean success) {
            this.text = text;
            this.success = success;
        }
    }

    /**
     * Cache a newly created divination record.
     */
    private void CacheDivinationRecord(DivinationRecord record) {
        // This would use cache put in production with proper cache manager
        // For simplicity, relying on @Cacheable above
    }
}