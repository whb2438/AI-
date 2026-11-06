package com.divination.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.divination.common.service.LoginLockService;
import com.divination.common.util.CardKeyUtil;
import com.divination.common.util.AiRequestUtil;
import com.divination.config.AiConfig;
import com.divination.common.exception.BusinessException;
import com.divination.common.result.ResultCode;
import com.divination.entity.*;
import com.divination.mapper.*;
import com.divination.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final CardKeyMapper cardKeyMapper;
    private final RecordMapper recordMapper;
    private final SignInRecordMapper signInRecordMapper;
    private final FortuneCalendarMapper fortuneCalendarMapper;
    private final PushNotificationMapper pushNotificationMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final NotificationMapper notificationMapper;
    private final UserFeedbackMapper userFeedbackMapper;
    private final IntegralShopMapper integralShopMapper;
    private final IntegralOrderMapper integralOrderMapper;
    private final UserLevelMapper userLevelMapper;
    private final CardKeyUtil cardKeyUtil;
    private final AiRequestUtil aiRequestUtil;
    private final AiConfig aiConfig;
    private final LoginLockService loginLockService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int registerGiveIntegral;
    public UserServiceImpl(PasswordEncoder passwordEncoder, CardKeyMapper cardKeyMapper, RecordMapper recordMapper,
            SignInRecordMapper signInRecordMapper, FortuneCalendarMapper fortuneCalendarMapper,
            PushNotificationMapper pushNotificationMapper, FavoriteRecordMapper favoriteRecordMapper,
            NotificationMapper notificationMapper, UserFeedbackMapper userFeedbackMapper,
            IntegralShopMapper integralShopMapper, IntegralOrderMapper integralOrderMapper,
            UserLevelMapper userLevelMapper, CardKeyUtil cardKeyUtil,
            AiRequestUtil aiRequestUtil, AiConfig aiConfig, LoginLockService loginLockService) {
        this.passwordEncoder = passwordEncoder;
        this.cardKeyMapper = cardKeyMapper;
        this.recordMapper = recordMapper;
        this.signInRecordMapper = signInRecordMapper;
        this.fortuneCalendarMapper = fortuneCalendarMapper;
        this.pushNotificationMapper = pushNotificationMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.notificationMapper = notificationMapper;
        this.userFeedbackMapper = userFeedbackMapper;
        this.integralShopMapper = integralShopMapper;
        this.integralOrderMapper = integralOrderMapper;
        this.userLevelMapper = userLevelMapper;
        this.cardKeyUtil = cardKeyUtil;
        this.aiRequestUtil = aiRequestUtil;
        this.aiConfig = aiConfig;
        this.loginLockService = loginLockService;
    }
    @org.springframework.beans.factory.annotation.Value("${divination.register-give-integral}")
    public void setRegisterGiveIntegral(int registerGiveIntegral) {
        this.registerGiveIntegral = registerGiveIntegral;
    }
    @Override @Transactional
    public User register(String username, String password, String ip) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (this.count(wrapper) > 0) throw new BusinessException(ResultCode.USERNAME_EXISTS);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setIntegral(registerGiveIntegral);
        user.setRole("USER");
        user.setStatus(1);
        user.setLoginIp(ip);
        user.setLoginCount(1);
        user.setLastLoginTime(LocalDateTime.now());
        user.setRegisterTime(LocalDateTime.now());
        this.save(user);
        return user;
    }
    @Override @Transactional
    public User login(String username, String password, String ip) {
        // Check if account is locked due to too many failed attempts
        if (loginLockService.isLocked(username)) {
            long remaining = loginLockService.getRemainingLockMinutes(username);
            throw new BusinessException(429, "账号因多次登录失败已被锁定，请 " + remaining + " 分钟后再试");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            // Record failed attempt
            int remainingLock = loginLockService.recordFailedAttempt(username);
            if (remainingLock > 0) {
                throw new BusinessException(429, "登录失败次数过多，账号已锁定15分钟");
            }
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }

        // Successful login — reset failed attempts
        loginLockService.resetAttempts(username);

        if (user.getStatus() == 0) throw new BusinessException(ResultCode.USER_DISABLED);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginIp(ip);
        user.setLoginCount(user.getLoginCount() + 1);
        this.updateById(user);
        return user;
    }
    @Override public User getUserById(Long userId) { return this.getById(userId); }
    @Override
    public IPage<User> listUsers(int page, int size, String username, String nickname, Integer status) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) wrapper.like(User::getUsername, username);
        if (nickname != null && !nickname.isEmpty()) wrapper.like(User::getNickname, nickname);
        if (status != null) wrapper.eq(User::getStatus, status);
        wrapper.orderByDesc(User::getRegisterTime);
        return this.page(pageParam, wrapper);
    }
    @Override @Transactional
    public void updateUser(User user) {
        if (user.getId() == null) return;
        User existing = this.getById(user.getId());
        if (existing == null) return;
        if (user.getStatus() != null && user.getStatus() == 0
                && existing.getStatus() != null && existing.getStatus() != 0) {
            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(User::getId, user.getId());
            Integer currentVersion = existing.getTokenVersion() != null ? existing.getTokenVersion() : 1;
            wrapper.set(User::getTokenVersion, currentVersion + 1);
            if (user.getNickname() != null) wrapper.set(User::getNickname, user.getNickname());
            if (user.getIntegral() != null) wrapper.set(User::getIntegral, user.getIntegral());
            if (user.getRole() != null) wrapper.set(User::getRole, user.getRole());
            wrapper.set(User::getStatus, user.getStatus());
            this.update(wrapper);
        } else {
            this.updateById(user);
        }
    }
    @Override @Transactional
    public void deleteUser(Long userId) {
        recordMapper.deleteByUserId(userId);
        cardKeyMapper.deleteByUserId(userId);
        this.removeById(userId);
    }
    @Override @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = this.getById(userId);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            Integer currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 1;
            user.setTokenVersion(currentVersion + 1);
            this.updateById(user);
        }
    }
    @Override @Transactional
    public void adjustIntegral(Long userId, int amount) {
        User user = this.getById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        int affected = this.baseMapper.adjustIntegralAtomically(userId, amount);
        if (affected == 0) throw new BusinessException(ResultCode.INTEGRAL_INSUFFICIENT);
    }
    @Override public long countTodayUsers() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(User::getRegisterTime, startOfDay);
        return this.count(wrapper);
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = this.getOne(wrapper);
        if (user == null) throw new UsernameNotFoundException("用户不存在: " + username);
        if (user.getStatus() == 0) throw new UsernameNotFoundException("账号已被禁用");
        Collection<GrantedAuthority> authorities = Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), authorities);
    }

    // ===================== NEW METHODS =====================

    // --- Sign-in ---
    @Override @Transactional
    public SignInRecord signIn(Long userId) {
        LocalDate today = LocalDate.now();
        SignInRecord existing = signInRecordMapper.selectOne(
                new LambdaQueryWrapper<SignInRecord>().eq(SignInRecord::getUserId, userId)
                        .eq(SignInRecord::getSignInDate, today));
        if (existing != null) throw new BusinessException("今日已签到");
        LocalDate yesterday = today.minusDays(1);
        SignInRecord yesterdayRecord = signInRecordMapper.selectOne(
                new LambdaQueryWrapper<SignInRecord>().eq(SignInRecord::getUserId, userId)
                        .eq(SignInRecord::getSignInDate, yesterday));
        int consecutive = (yesterdayRecord != null) ? yesterdayRecord.getConsecutiveDays() + 1 : 1;
        int reward = 5 + (consecutive >= 7 ? 10 : consecutive >= 3 ? 5 : 0);
        SignInRecord record = new SignInRecord();
        record.setUserId(userId);
        record.setSignInDate(today);
        record.setConsecutiveDays(consecutive);
        record.setRewardIntegral(reward);
        signInRecordMapper.insert(record);
        this.baseMapper.addIntegralAtomically(userId, reward);
        // Generate Liu Yao fortune for today after successful sign-in
        generateLiuYaoFortune(userId, today);
        return record;
    }

    /**
     * Generate a Liu Yao (六爻) hexagram prediction via AI and store it in the fortune calendar.
     */
    private void generateLiuYaoFortune(Long userId, LocalDate date) {
        try {
            String systemPrompt = "你是精通六爻占卜的资深玄学命理师。根据用户当日签到的日期信息，模拟掷硬币起卦，" +
                    "给出卦象名称、爻辞解读和当日运势建议。文风古风典雅。请严格按以下JSON格式返回（不要返回其他内容）：" +
                    "{\"hexagram\":\"卦名\",\"yao\":\"爻辞解读\",\"overallScore\":6,\"careerScore\":7,\"wealthScore\":8,\"loveScore\":6,\"healthScore\":7," +
                    "\"fortuneText\":\"运势分析文字\",\"luckyColor\":\"幸运颜色\",\"luckyNumber\":\"幸运数字\",\"luckyDirection\":\"幸运方向\"}";
            String userPrompt = "请为" + date + "日的用户起一卦六爻占卜，给出当日运势。";

            String reply = aiRequestUtil.callAi(systemPrompt, userPrompt);
            // Parse JSON from AI response (handle markdown code blocks)
            String json = reply;
            // Strip markdown code block wrappers
            int codeStart = json.indexOf("```");
            if (codeStart >= 0) {
                int braceStart = json.indexOf('{', codeStart + 3);
                int braceEnd = json.lastIndexOf('}');
                if (braceStart >= 0 && braceEnd > braceStart) {
                    json = json.substring(braceStart, braceEnd + 1);
                }
            } else {
                int braceStart = json.indexOf('{');
                int braceEnd = json.lastIndexOf('}');
                if (braceStart >= 0 && braceEnd > braceStart) {
                    json = json.substring(braceStart, braceEnd + 1);
                }
            }
            // Validate JSON is not empty after extraction
            if (json.length() <= 2) {
                json = "";
            }

            FortuneCalendar fortune = new FortuneCalendar();
            fortune.setUserId(userId);
            fortune.setFortuneDate(date);
            fortune.setCreateTime(LocalDateTime.now());

            // Fallback random scores if AI fails
            fortune.setOverallScore(6);
            fortune.setCareerScore(6);
            fortune.setWealthScore(6);
            fortune.setLoveScore(6);
            fortune.setHealthScore(6);
            fortune.setFortuneText("今日六爻占卜卦象已起，运势平顺，宜修身养性，静待时机。");
            fortune.setLuckyColor("金色");
            fortune.setLuckyNumber("6");
            fortune.setLuckyDirection("正西");

            if (json.contains("hexagram")) {
                try {
                    JsonNode node = objectMapper.readTree(json);
                    String hexagram = node.has("hexagram") ? node.get("hexagram").asText() : "未知卦象";
                    String yao = node.has("yao") ? node.get("yao").asText() : "";
                    String fortuneText = node.has("fortuneText") ? node.get("fortuneText").asText() : "";
                    // Sanitize: if fortuneText starts with { and contains "hexagram" as key,
                    // it means the AI returned the whole JSON as text — extract actual fields instead
                    if (fortuneText.trim().startsWith("{")) {
                        try {
                            JsonNode inner = objectMapper.readTree(fortuneText);
                            fortuneText = inner.has("fortuneText") ? inner.get("fortuneText").asText("") : "";
                        } catch (Exception ignored) {
                            fortuneText = "";
                        }
                    }
                    String textPrefix = "今日六爻起卦：" + hexagram + " · " + yao;
                    if (!fortuneText.isEmpty()) {
                        // Unescape backslash-n to real newlines for proper display
                        fortuneText = fortuneText.replace("\\n", "\n");
                        fortune.setFortuneText(textPrefix + "\n" + fortuneText);
                    } else {
                        fortune.setFortuneText(textPrefix);
                    }
                    fortune.setOverallScore(node.has("overallScore") ? node.get("overallScore").asInt(6) : 6);
                    fortune.setCareerScore(node.has("careerScore") ? node.get("careerScore").asInt(6) : 6);
                    fortune.setWealthScore(node.has("wealthScore") ? node.get("wealthScore").asInt(6) : 6);
                    fortune.setLoveScore(node.has("loveScore") ? node.get("loveScore").asInt(6) : 6);
                    fortune.setHealthScore(node.has("healthScore") ? node.get("healthScore").asInt(6) : 6);
                    fortune.setLuckyColor(node.has("luckyColor") ? node.get("luckyColor").asText("金色") : "金色");
                    fortune.setLuckyNumber(node.has("luckyNumber") ? node.get("luckyNumber").asText("6") : "6");
                    fortune.setLuckyDirection(node.has("luckyDirection") ? node.get("luckyDirection").asText("正西") : "正西");
                } catch (Exception ignored) {
                    // Use defaults
                }
            }

            // Upsert: delete existing and re-insert, or update
            FortuneCalendar existing = fortuneCalendarMapper.selectOne(
                    new LambdaQueryWrapper<FortuneCalendar>()
                            .eq(FortuneCalendar::getUserId, userId)
                            .eq(FortuneCalendar::getFortuneDate, date));
            if (existing != null) {
                fortune.setId(existing.getId());
                fortuneCalendarMapper.updateById(fortune);
            } else {
                fortuneCalendarMapper.insert(fortune);
            }
        } catch (Exception e) {
            // Fallback: generate random fortune if AI fails
            generateFallbackFortune(userId, date);
        }
    }

    private int extractScore(String json, String key, int fallback) {
        try {
            int start = json.indexOf('"' + key + '"') + key.length() + 3;
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return Math.max(1, Math.min(10, Integer.parseInt(json.substring(start, end))));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String extractString(String json, String key, String fallback) {
        try {
            int start = json.indexOf('"' + key + "\"\":\"");
            if (start < 0) return fallback;
            start += key.length() + 4;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        } catch (Exception e) {
            return fallback;
        }
    }

    private void generateFallbackFortune(Long userId, LocalDate date) {
        Random rand = new Random();
        FortuneCalendar fortune = new FortuneCalendar();
        fortune.setUserId(userId);
        fortune.setFortuneDate(date);
        fortune.setCreateTime(LocalDateTime.now());
        int base = rand.nextInt(4) + 5; // 5-8
        fortune.setOverallScore(base);
        fortune.setCareerScore(rand.nextInt(4) + 5);
        fortune.setWealthScore(rand.nextInt(4) + 5);
        fortune.setLoveScore(rand.nextInt(4) + 5);
        fortune.setHealthScore(rand.nextInt(4) + 5);
        fortune.setFortuneText("今日六爻占卜：天道酬勤，顺势而为。");
        String[] colors = {"金色", "红色", "青色", "白色", "黑色"};
        fortune.setLuckyColor(colors[rand.nextInt(colors.length)]);
        fortune.setLuckyNumber(String.valueOf(rand.nextInt(9) + 1));
        String[] dirs = {"正东", "正南", "正西", "正北", "东南", "西南"};
        fortune.setLuckyDirection(dirs[rand.nextInt(dirs.length)]);

        FortuneCalendar existing = fortuneCalendarMapper.selectOne(
                new LambdaQueryWrapper<FortuneCalendar>()
                        .eq(FortuneCalendar::getUserId, userId)
                        .eq(FortuneCalendar::getFortuneDate, date));
        if (existing != null) {
            fortune.setId(existing.getId());
            fortuneCalendarMapper.updateById(fortune);
        } else {
            fortuneCalendarMapper.insert(fortune);
        }
    }
    @Override public Map<String, Object> getSignInStatus(Long userId) {
        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();
        SignInRecord todayRecord = signInRecordMapper.selectOne(
                new LambdaQueryWrapper<SignInRecord>().eq(SignInRecord::getUserId, userId)
                        .eq(SignInRecord::getSignInDate, today));
        data.put("hasSignedIn", todayRecord != null);
        if (todayRecord != null) {
            data.put("consecutiveDays", todayRecord.getConsecutiveDays());
            data.put("rewardIntegral", todayRecord.getRewardIntegral());
        } else {
            LocalDate yesterday = today.minusDays(1);
            SignInRecord yesterdayRecord = signInRecordMapper.selectOne(
                    new LambdaQueryWrapper<SignInRecord>().eq(SignInRecord::getUserId, userId)
                            .eq(SignInRecord::getSignInDate, yesterday));
            data.put("consecutiveDays", yesterdayRecord != null ? yesterdayRecord.getConsecutiveDays() : 0);
        }
        return data;
    }

    // --- Fortune Calendar ---
    @Override @Transactional
    public FortuneCalendar generateDailyFortune(Long userId) {
        LocalDate today = LocalDate.now();
        // Check if already generated
        FortuneCalendar existing = fortuneCalendarMapper.getByDate(userId, today);
        if (existing != null) return existing;
        // Generate random fortune scores
        Random random = new Random();
        FortuneCalendar fortune = new FortuneCalendar();
        fortune.setUserId(userId);
        fortune.setFortuneDate(today);
        fortune.setOverallScore(random.nextInt(6) + 5); // 5-10
        fortune.setCareerScore(random.nextInt(6) + 5);
        fortune.setWealthScore(random.nextInt(6) + 5);
        fortune.setLoveScore(random.nextInt(6) + 5);
        fortune.setHealthScore(random.nextInt(6) + 5);
        // Generate lucky attributes
        String[] colors = {"红色", "金色", "蓝色", "绿色", "紫色", "白色"};
        String[] directions = {"东", "南", "西", "北", "东南", "西南", "东北", "西北"};
        fortune.setLuckyColor(colors[random.nextInt(colors.length)]);
        fortune.setLuckyNumber(String.valueOf(random.nextInt(99) + 1));
        fortune.setLuckyDirection(directions[random.nextInt(directions.length)]);
        fortune.setFortuneText("今日运势平稳，宜修身养性，静待时机。");
        fortuneCalendarMapper.insert(fortune);
        // Create notification
        createNotification(userId, "每日运势", "您的今日运势已生成，综合评分 " + fortune.getOverallScore() + "/10", "PUSH");
        return fortune;
    }
    @Override
    public List<FortuneCalendar> getFortuneRange(Long userId, LocalDate start, LocalDate end) {
        return fortuneCalendarMapper.getFortuneRange(userId, start, end);
    }
    @Override
    public FortuneCalendar getFortuneByDate(Long userId, LocalDate date) {
        return fortuneCalendarMapper.getByDate(userId, date);
    }

    // --- Push Settings ---
    @Override public PushNotification getPushSetting(Long userId) {
        PushNotification setting = pushNotificationMapper.selectOne(
                new LambdaQueryWrapper<PushNotification>().eq(PushNotification::getUserId, userId));
        if (setting == null) {
            setting = new PushNotification();
            setting.setUserId(userId);
            setting.setEnableFortune(1);
            setting.setEnablePromotion(0);
            pushNotificationMapper.insert(setting);
        }
        return setting;
    }
    @Override @Transactional
    public void togglePushSetting(Long userId, String settingKey, boolean enabled) {
        PushNotification setting = getPushSetting(userId);
        if ("enable_fortune".equals(settingKey)) {
            setting.setEnableFortune(enabled ? 1 : 0);
        } else if ("enable_promotion".equals(settingKey)) {
            setting.setEnablePromotion(enabled ? 1 : 0);
        }
        pushNotificationMapper.updateById(setting);
    }

    // --- Favorites ---
    @Override @Transactional
    public void createFavorite(Long userId, Long recordId) {
        // Check if already favorited
        FavoriteRecord existing = favoriteRecordMapper.selectOne(
                new LambdaQueryWrapper<FavoriteRecord>()
                        .eq(FavoriteRecord::getUserId, userId)
                        .eq(FavoriteRecord::getRecordId, recordId));
        if (existing != null) throw new BusinessException("已收藏此记录");
        FavoriteRecord favorite = new FavoriteRecord();
        favorite.setUserId(userId);
        favorite.setRecordId(recordId);
        favoriteRecordMapper.insert(favorite);
    }
    @Override @Transactional
    public void removeFavorite(Long userId, Long recordId) {
        favoriteRecordMapper.delete(new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getUserId, userId)
                .eq(FavoriteRecord::getRecordId, recordId));
    }
    @Override public boolean isFavorite(Long userId, Long recordId) {
        return favoriteRecordMapper.selectCount(
                new LambdaQueryWrapper<FavoriteRecord>()
                        .eq(FavoriteRecord::getUserId, userId)
                        .eq(FavoriteRecord::getRecordId, recordId)) > 0;
    }
    @Override
    public List<DivinationRecord> getFavoriteRecords(Long userId) {
        List<FavoriteRecord> favorites = favoriteRecordMapper.selectList(
                new LambdaQueryWrapper<FavoriteRecord>()
                        .eq(FavoriteRecord::getUserId, userId)
                        .orderByDesc(FavoriteRecord::getCreateTime));
        List<Long> recordIds = favorites.stream()
                .map(FavoriteRecord::getRecordId)
                .collect(Collectors.toList());
        if (recordIds.isEmpty()) return Collections.emptyList();
        return recordMapper.selectBatchIds(recordIds);
    }

    // --- Notifications ---
    @Override @Transactional
    public void createNotification(Long userId, String title, String content, String type) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }
    @Override @Transactional
    public void markNotificationRead(Long userId, Long notificationId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationEntity>()
                .eq(NotificationEntity::getId, notificationId)
                .eq(NotificationEntity::getUserId, userId)
                .set(NotificationEntity::getIsRead, 1));
    }
    @Override @Transactional
    public void markAllNotificationsRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<NotificationEntity>()
                .eq(NotificationEntity::getUserId, userId)
                .set(NotificationEntity::getIsRead, 1));
    }
    @Override public int getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
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
    public IPage<NotificationEntity> listAllNotifications(int page, int size, String type) {
        Page<NotificationEntity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<NotificationEntity> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) wrapper.eq(NotificationEntity::getType, type);
        wrapper.orderByDesc(NotificationEntity::getCreateTime);
        return notificationMapper.selectPage(pageParam, wrapper);
    }
    @Override @Transactional
    public void broadcastNotification(String title, String content, String type) {
        // Get all active users
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        List<User> users = this.list(wrapper);
        for (User user : users) {
            createNotification(user.getId(), title, content, type);
        }
    }

    // --- Feedback ---
    @Override @Transactional
    public void submitFeedback(Long userId, String feedbackType, String content, String contact) {
        UserFeedback feedback = new UserFeedback();
        feedback.setUserId(userId);
        feedback.setFeedbackType(feedbackType);
        feedback.setContent(content);
        feedback.setContact(contact != null ? contact : "");
        feedback.setStatus(0);
        userFeedbackMapper.insert(feedback);
    }
    @Override
    public IPage<UserFeedback> getUserFeedbackList(Long userId, int page, int size) {
        Page<UserFeedback> pageParam = new Page<>(page, size);
        return userFeedbackMapper.selectPage(pageParam,
                new LambdaQueryWrapper<UserFeedback>()
                        .eq(UserFeedback::getUserId, userId)
                        .orderByDesc(UserFeedback::getCreateTime));
    }
    @Override
    public IPage<UserFeedback> listAllFeedback(int page, int size, Integer status) {
        Page<UserFeedback> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(UserFeedback::getStatus, status);
        wrapper.orderByDesc(UserFeedback::getCreateTime);
        return userFeedbackMapper.selectPage(pageParam, wrapper);
    }
    @Override @Transactional
    public void replyFeedback(Long id, String reply) {
        userFeedbackMapper.update(null, new LambdaUpdateWrapper<UserFeedback>()
                .eq(UserFeedback::getId, id)
                .set(UserFeedback::getReply, reply)
                .set(UserFeedback::getStatus, 1));
    }

    // --- Shop ---
    @Override
    public List<IntegralShop> getShopItems(Integer enabled) {
        LambdaQueryWrapper<IntegralShop> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) wrapper.eq(IntegralShop::getEnabled, enabled);
        wrapper.orderByAsc(IntegralShop::getSortOrder);
        return integralShopMapper.selectList(wrapper);
    }
    @Override @Transactional
    public Map<String, Object> redeemShopItem(Long userId, Long shopItemId) {
        IntegralShop shopItem = integralShopMapper.selectById(shopItemId);
        if (shopItem == null) throw new BusinessException("商品不存在");
        if (shopItem.getEnabled() == 0) throw new BusinessException("商品已下架");
        User user = this.getById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        if (user.getIntegral() < shopItem.getCostIntegral()) {
            throw new BusinessException(ResultCode.INTEGRAL_INSUFFICIENT);
        }
        // Check stock (stock < 0 means unlimited, stock == 0 means sold out)
        if (shopItem.getStock() >= 0 && shopItem.getStock() <= 0) {
            throw new BusinessException("库存不足");
        }
        // Deduct integral
        this.baseMapper.adjustIntegralAtomically(userId, -shopItem.getCostIntegral());
        // Create order
        IntegralOrder order = new IntegralOrder();
        order.setUserId(userId);
        order.setShopItemId(shopItemId);
        order.setCostIntegral(shopItem.getCostIntegral());
        order.setStatus(1);
        String remark = "";
        if ("CARD_KEY".equals(shopItem.getItemType())) {
            String cardKey = cardKeyUtil.generateCardKey();
            remark = cardKey;
            order.setRemark(cardKey);
            // Save card key to database
            CardKey cardKeyEntity = new CardKey();
            cardKeyEntity.setCardKey(cardKey);
            cardKeyEntity.setIntegral(shopItem.getCostIntegral());
            cardKeyEntity.setBatchNo("SHOP-" + System.currentTimeMillis());
            cardKeyEntity.setStatus(1);
            cardKeyEntity.setUserId(userId);
            cardKeyEntity.setExchangeTime(LocalDateTime.now());
            cardKeyMapper.insert(cardKeyEntity);
        } else if ("VIP".equals(shopItem.getItemType())) {
            // Grant VIP: set expiration to 30 days from now
            user.setVipExpiresAt(LocalDateTime.now().plusDays(30));
            this.updateById(user);
            remark = "至尊VIP月卡（30天）";
        } else if ("EXTRA_LIMIT".equals(shopItem.getItemType())) {
            // Add integral as bonus
            this.baseMapper.addIntegralAtomically(userId, shopItem.getCostIntegral());
        }
        integralOrderMapper.insert(order);
        // Decrement stock (stock < 0 means unlimited)
        if (shopItem.getStock() > 0) {
            integralShopMapper.update(null, new LambdaUpdateWrapper<IntegralShop>()
                    .eq(IntegralShop::getId, shopItemId)
                    .setSql("stock = stock - 1"));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("remark", remark);
        return result;
    }
    @Override @Transactional
    public void addShopItem(IntegralShop shopItem) {
        integralShopMapper.insert(shopItem);
    }
    @Override @Transactional
    public void updateShopItem(IntegralShop shopItem) {
        integralShopMapper.updateById(shopItem);
    }

    // --- User Level ---
    @Override public Map<String, Object> getUserLevelData(Long userId) {
        Map<String, Object> data = new HashMap<>();
        UserLevel level = userLevelMapper.selectOne(
                new LambdaQueryWrapper<UserLevel>().eq(UserLevel::getUserId, userId));
        if (level == null) {
            level = new UserLevel();
            level.setUserId(userId);
            level.setLevel("新手");
            level.setLevelPoints(0);
            level.setTotalDivinationCount(0);
        }
        data.put("level", level.getLevel());
        data.put("levelPoints", level.getLevelPoints());
        data.put("totalDivinationCount", level.getTotalDivinationCount());
        // Get user's current integral too
        User user = this.getById(userId);
        if (user != null) data.put("currentIntegral", user.getIntegral());
        return data;
    }
    @Override @Transactional
    public void updateLevelAfterDivination(Long userId, int costIntegral) {
        UserLevel level = userLevelMapper.selectOne(
                new LambdaQueryWrapper<UserLevel>().eq(UserLevel::getUserId, userId));
        if (level == null) {
            level = new UserLevel();
            level.setUserId(userId);
            level.setLevelPoints(costIntegral);
            level.setLevel("新手");
            level.setTotalDivinationCount(1);
            userLevelMapper.insert(level);
        } else {
            level.setLevelPoints(level.getLevelPoints() + costIntegral);
            level.setTotalDivinationCount(level.getTotalDivinationCount() + 1);
            // Level thresholds
            if (level.getLevelPoints() >= 1000) level.setLevel("宗师");
            else if (level.getLevelPoints() >= 500) level.setLevel("大师");
            else if (level.getLevelPoints() >= 200) level.setLevel("熟练");
            else if (level.getLevelPoints() >= 50) level.setLevel("入门");
            userLevelMapper.updateById(level);
        }
    }

    // --- Divination Trend ---
    @Override
    public Map<String, Object> getDivinationTrend(Long userId, int days) {
        Map<String, Object> data = new HashMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        // Count records per day
        List<DivinationRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<DivinationRecord>()
                        .eq(DivinationRecord::getUserId, userId)
                        .ge(DivinationRecord::getCreateTime, startDate.atStartOfDay())
                        .le(DivinationRecord::getCreateTime, endDate.plusDays(1).atStartOfDay()));
        // Group by date
        Map<String, Long> countByDate = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreateTime().toLocalDate().toString(),
                        Collectors.counting()));
        // Build chart data
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String dateStr = startDate.plusDays(i).toString();
            long count = countByDate.getOrDefault(dateStr, 0L);
            Map<String, Object> point = new HashMap<>();
            point.put("date", dateStr);
            point.put("count", count);
            chartData.add(point);
        }
        data.put("chartData", chartData);

        // Type breakdown: group by type and date
        Map<String, Map<String, Long>> countByTypeAndDate = records.stream()
                .collect(Collectors.groupingBy(
                        DivinationRecord::getType,
                        Collectors.groupingBy(
                                r -> r.getCreateTime().toLocalDate().toString(),
                                Collectors.counting())));

        Map<String, List<Map<String, Object>>> typeBreakdown = new HashMap<>();
        for (Map.Entry<String, Map<String, Long>> entry : countByTypeAndDate.entrySet()) {
            String typeName = entry.getKey();
            List<Map<String, Object>> typeData = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                String dateStr = startDate.plusDays(i).toString();
                long count = entry.getValue().getOrDefault(dateStr, 0L);
                Map<String, Object> point = new HashMap<>();
                point.put("date", dateStr);
                point.put("count", count);
                typeData.add(point);
            }
            typeBreakdown.put(typeName, typeData);
        }
        data.put("typeBreakdown", typeBreakdown);
        return data;
    }

    // --- Backpack ---
    @Override
    public List<CardKey> getBackpack(Long userId) {
        return cardKeyMapper.selectList(
                new LambdaQueryWrapper<CardKey>()
                        .eq(CardKey::getUserId, userId)
                        .eq(CardKey::getStatus, 1)
                        .orderByDesc(CardKey::getExchangeTime));
    }
    @Override @Transactional
    public void useCardKey(Long userId, Long cardKeyId) {
        CardKey key = cardKeyMapper.selectById(cardKeyId);
        if (key == null || !key.getUserId().equals(userId)) {
            throw new BusinessException("卡密不存在");
        }
        if (key.getStatus() != 1) {
            throw new BusinessException("该卡密已使用");
        }
        // Mark as used (status 2 = used)
        cardKeyMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CardKey>()
                .eq(CardKey::getId, cardKeyId)
                .set(CardKey::getStatus, 2));
        // Add integral from the card key atomically
        this.baseMapper.addIntegralAtomically(userId, key.getIntegral());
    }

    @Override
    public void updateUserProfile(Long userId, String realName, String gender,
                                  String birthDate, String birthTime) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRealName(realName);
        user.setGender(gender);
        user.setBirthDate(birthDate);
        user.setBirthTime(birthTime);
        updateById(user);
    }
}
