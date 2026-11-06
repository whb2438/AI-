package com.divination.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.divination.common.result.Result;
import com.divination.common.util.JwtUtil;
import com.divination.entity.*;
import com.divination.service.DivinationRecordService;
import com.divination.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final DivinationRecordService divinationService;
    private final JwtUtil jwtUtil;
    public UserController(UserService userService, DivinationRecordService divinationService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.divinationService = divinationService;
        this.jwtUtil = jwtUtil;
    }
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Result.error(400, "两次密码输入不一致");
        }
        User user = userService.register(request.getUsername(), request.getPassword(), null);
        Integer tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 1;
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), tokenVersion);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("integral", user.getIntegral());
        data.put("role", user.getRole());
        data.put("vipExpiresAt", user.getVipExpiresAt());
        data.put("isVip", user.isVipActive());
        return Result.success("注册成功", data);
    }
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest) {
        User user = userService.login(request.getUsername(), request.getPassword(),
                httpRequest.getRemoteAddr());
        Integer tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 1;
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), tokenVersion);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("integral", user.getIntegral());
        data.put("role", user.getRole());
        data.put("vipExpiresAt", user.getVipExpiresAt());
        data.put("isVip", user.isVipActive());
        return Result.success("登录成功", data);
    }
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        User user = userService.getUserById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("integral", user.getIntegral());
        data.put("role", user.getRole());
        data.put("realName", user.getRealName());
        data.put("gender", user.getGender());
        data.put("birthDate", user.getBirthDate());
        data.put("birthTime", user.getBirthTime());
        data.put("registerTime", user.getRegisterTime());
        data.put("vipExpiresAt", user.getVipExpiresAt());
        data.put("isVip", user.isVipActive());
        return Result.success(data);
    }
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        return Result.success("退出登录成功");
    }
    @GetMapping("/records")
    public Result<?> getRecords(HttpServletRequest request,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") @Max(50) int size) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        var records = divinationService.listRecords(userId, page, size);
        return Result.success(records);
    }

    // ===================== NEW ENDPOINTS =====================

    // --- Sign-in ---
    @PostMapping("/signin")
    public Result<Map<String, Object>> signIn(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        SignInRecord record = userService.signIn(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("consecutiveDays", record.getConsecutiveDays());
        data.put("rewardIntegral", record.getRewardIntegral());
        return Result.success("签到成功", data);
    }
    @GetMapping("/signin/status")
    public Result<Map<String, Object>> getSignInStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.getSignInStatus(userId));
    }

    // --- Fortune Calendar ---
    @GetMapping("/fortune/calendar")
    public Result<List<FortuneCalendar>> getFortuneCalendar(HttpServletRequest request,
            @RequestParam String year, @RequestParam String month) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        LocalDate start = LocalDate.parse(year + "-" + month + "-01");
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        return Result.success(userService.getFortuneRange(userId, start, end));
    }
    @PostMapping("/fortune/generate")
    public Result<FortuneCalendar> generateFortune(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.generateDailyFortune(userId));
    }

    // --- Push Settings ---
    @GetMapping("/push/settings")
    public Result<PushNotification> getPushSettings(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.getPushSetting(userId));
    }
    @PutMapping("/push/settings")
    public Result<Void> updatePushSettings(HttpServletRequest httpRequest,
            @RequestBody PushSettingRequest settingRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.togglePushSetting(userId, settingRequest.getSettingKey(), settingRequest.isEnabled());
        return Result.success();
    }

    // --- Favorites ---
    @PostMapping("/favorite/{recordId}")
    public Result<Void> addFavorite(HttpServletRequest request, @PathVariable Long recordId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.createFavorite(userId, recordId);
        return Result.success();
    }
    @DeleteMapping("/favorite/{recordId}")
    public Result<Void> removeFavorite(HttpServletRequest request, @PathVariable Long recordId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.removeFavorite(userId, recordId);
        return Result.success();
    }
    @GetMapping("/favorites")
    public Result<List<DivinationRecord>> getFavorites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.getFavoriteRecords(userId));
    }
    @GetMapping("/favorite/check/{recordId}")
    public Result<Boolean> checkFavorite(HttpServletRequest request, @PathVariable Long recordId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.isFavorite(userId, recordId));
    }

    // --- Notifications ---
    @GetMapping("/notifications")
    public Result<?> getNotifications(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        var notifications = userService.getNotifications(userId, page, size);
        int unread = userService.getUnreadCount(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("notifications", notifications);
        data.put("unreadCount", unread);
        return Result.success(data);
    }
    @PostMapping("/notification/{id}/read")
    public Result<Void> markNotificationRead(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.markNotificationRead(userId, id);
        return Result.success();
    }
    @PostMapping("/notifications/read-all")
    public Result<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.markAllNotificationsRead(userId);
        return Result.success();
    }

    // --- Feedback ---
    @PostMapping("/feedback")
    public Result<String> submitFeedback(HttpServletRequest httpRequest,
            @RequestBody FeedbackRequest feedbackRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.submitFeedback(userId, feedbackRequest.getFeedbackType(), feedbackRequest.getContent(), feedbackRequest.getContact());
        return Result.success("反馈已提交", null);
    }
    @GetMapping("/feedback/list")
    public Result<IPage<UserFeedback>> getUserFeedback(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.getUserFeedbackList(userId, page, size));
    }

    // --- Shop ---
    @GetMapping("/shop")
    public Result<List<IntegralShop>> getShopItems(@RequestParam(required = false) Integer enabled) {
        return Result.success(userService.getShopItems(enabled));
    }
    @PostMapping("/shop/redeem/{itemId}")
    public Result<Map<String, Object>> redeemShopItem(HttpServletRequest request, @PathVariable Long itemId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.redeemShopItem(userId, itemId));
    }

    // --- User Level ---
    @GetMapping("/level")
    public Result<Map<String, Object>> getUserLevel(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.getUserLevelData(userId));
    }

    // --- Divination Trend ---
    @GetMapping("/records/trend")
    public Result<Map<String, Object>> getDivinationTrend(HttpServletRequest request,
            @RequestParam(defaultValue = "30") int days) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.getDivinationTrend(userId, days));
    }

    // --- Backpack (Card Keys) ---
    @GetMapping("/backpack")
    public Result<List<CardKey>> getBackpack(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        return Result.success(userService.getBackpack(userId));
    }
    @PostMapping("/backpack/use/{id}")
    public Result<Void> useCardKey(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.useCardKey(userId, id);
        return Result.success();
    }

    // --- User Profile ---
    @GetMapping("/profile")
    public Result<Map<String, Object>> getUserProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        User user = userService.getUserById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("realName", user.getRealName());
        data.put("gender", user.getGender());
        data.put("birthDate", user.getBirthDate());
        data.put("birthTime", user.getBirthTime());
        data.put("role", user.getRole());
        data.put("vipExpiresAt", user.getVipExpiresAt());
        data.put("isVip", user.isVipActive());
        return Result.success(data);
    }

    @PutMapping("/profile")
    public Result<Void> updateUserProfile(HttpServletRequest httpRequest,
            @Valid @RequestBody UserProfileRequest profileRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        userService.updateUserProfile(userId, profileRequest.getRealName(), profileRequest.getGender(),
                profileRequest.getBirthDate(), profileRequest.getBirthTime());
        return Result.success();
    }

    // ===================== INNER DTOs =====================
    public static class PushSettingRequest {
        private String settingKey;
        private boolean enabled;
        public String getSettingKey() { return settingKey; }
        public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    public static class FeedbackRequest {
        @NotBlank(message = "反馈类型不能为空")
        private String feedbackType;
        @NotBlank(message = "反馈内容不能为空")
        private String content;
        private String contact;
        public String getFeedbackType() { return feedbackType; }
        public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }
    }
    public static class UserProfileRequest {
        @Size(max = 50) private String realName;
        @Pattern(regexp = "^(男|女)$") private String gender;
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") private String birthDate;
        @Pattern(regexp = "^(子|丑|寅|卯|辰|巳|午|未|申|酉|戌|亥)时$") private String birthTime;
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getBirthDate() { return birthDate; }
        public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
        public String getBirthTime() { return birthTime; }
        public void setBirthTime(String birthTime) { this.birthTime = birthTime; }
    }
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
        private String username;
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,32}$",
                 message = "密码必须包含大小写字母和数字，长度8-32位")
        private String password;
        @NotBlank(message = "确认密码不能为空")
        @Size(min = 8, max = 100, message = "确认密码长度必须在8-100之间")
        private String confirmPassword;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
