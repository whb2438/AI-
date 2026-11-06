package com.divination.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.divination.common.exception.BusinessException;
import com.divination.common.result.Result;
import com.divination.entity.IntegralShop;
import com.divination.entity.NotificationEntity;
import com.divination.entity.User;
import com.divination.entity.UserFeedback;
import com.divination.service.AdminService;
import com.divination.service.CardService;
import com.divination.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;
    private final CardService cardService;
    private final AdminService adminService;
    public AdminController(UserService userService, CardService cardService, AdminService adminService) {
        this.userService = userService;
        this.cardService = cardService;
        this.adminService = adminService;
    }
    /**
     * GET /api/admin/user/list?page=1&size=10&username=&status=
     * NOTE: password field is always masked for security (BUG-FIX-002).
     */
    @GetMapping("/user/list")
    public Result<IPage<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String nickname,
            @RequestParam(required = false) Integer status) {
        checkAdmin();
        IPage<User> users = userService.listUsers(page, size, username, nickname, status);
        // Mask passwords in returned user data
        users.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
    /**
     * PUT /api/admin/user/update
     */
    @PutMapping("/user/update")
    public Result<Void> updateUser(@Valid @RequestBody UpdateUserRequest request) {
        checkAdmin();
        if (request.getId() == null) {
            return Result.error(400, "用户ID不能为空");
        }
        Long currentUserId = getCurrentUserId();
        if (request.getId().equals(currentUserId)) {
            return Result.error(400, "不能操作自己");
        }
        User user = new User();
        user.setId(request.getId());
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getIntegral() != null) user.setIntegral(request.getIntegral());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getIntegral() != null && request.getStatus() != null && request.getStatus() == 0) {
            // 禁用用户时通过 updateUser 递增 tokenVersion，强制踢出所有已登录 session
            userService.updateUser(user);
        } else {
            userService.updateById(user);
        }
        return Result.success();
    }
    /**
     * DELETE /api/admin/user/delete/{id}
     */
    @DeleteMapping("/user/delete/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        checkAdmin();
        Long currentUserId = getCurrentUserId();
        if (id.equals(currentUserId)) {
            return Result.error(400, "不能删除自己");
        }
        userService.deleteUser(id);
        return Result.success();
    }
    /**
     * POST /api/admin/card/generate
     */
    @PostMapping("/card/generate")
    public Result<List<String>> generateCardKeys(@Valid @RequestBody GenerateCardRequest request) {
        checkAdmin();
        List<String> keys = cardService.generateCardKeys(
                request.getCount(), request.getIntegral(), request.getBatchNo());
        return Result.success(keys);
    }
    /**
     * GET /api/admin/card/list?page=1&size=10&batchNo=&status=
     */
    @GetMapping("/card/list")
    public Result<IPage<com.divination.entity.CardKey>> listCardKeys(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String batchNo,
            @RequestParam(required = false) Integer status) {
        checkAdmin();
        return Result.success(cardService.listCardKeys(page, size, batchNo, status));
    }
    /**
     * PUT /api/admin/card/invalid/{id}
     */
    @PutMapping("/card/invalid/{id}")
    public Result<Void> invalidateCard(@PathVariable Long id) {
        checkAdmin();
        cardService.invalidateCardKey(id);
        return Result.success();
    }
    @DeleteMapping("/card/delete/{id}")
    public Result<Void> deleteCard(@PathVariable Long id) {
        checkAdmin();
        cardService.deleteCardKey(id);
        return Result.success();
    }
    /**
     * GET /api/admin/statistics
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        checkAdmin();
        return Result.success(adminService.getStatistics());
    }
    /**
     * GET /api/admin/stat/trend?days=30
     */
    @GetMapping("/stat/trend")
    public Result<Map<String, Object>> getTrendData(
            @RequestParam(defaultValue = "30") int days) {
        checkAdmin();
        return Result.success(adminService.getTrendData(days));
    }
    /**
     * PUT /api/admin/user/reset-password
     */
    @PutMapping("/user/reset-password")
    public Result<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        checkAdmin();
        if (request.getId() == null || request.getPassword() == null || request.getPassword().isEmpty()) {
            return Result.error(400, "用户ID和新密码不能为空");
        }
        Long currentUserId = getCurrentUserId();
        if (request.getId().equals(currentUserId)) {
            return Result.error(400, "不能重置自己密码");
        }
        userService.resetPassword(request.getId(), request.getPassword());
        return Result.success();
    }

    // --- Admin: Notification Management ---
    @GetMapping("/notification/list")
    public Result<IPage<NotificationEntity>> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type) {
        checkAdmin();
        return Result.success(adminService.listNotifications(page, size, type));
    }
    @PostMapping("/notification/broadcast")
    public Result<Void> broadcastNotification(@RequestBody BroadcastRequest request) {
        checkAdmin();
        adminService.broadcastNotification(request.getTitle(), request.getContent(), request.getType());
        return Result.success();
    }

    // --- Admin: Feedback Management ---
    @GetMapping("/feedback/list")
    public Result<IPage<UserFeedback>> listFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        checkAdmin();
        return Result.success(adminService.listFeedback(page, size, status));
    }
    @PutMapping("/feedback/reply")
    public Result<Void> replyFeedback(@RequestBody ReplyFeedbackRequest request) {
        checkAdmin();
        adminService.replyFeedback(request.getId(), request.getReply());
        return Result.success();
    }

    // --- Admin: Shop Management ---
    @PostMapping("/shop/item")
    public Result<Void> addShopItem(@Valid @RequestBody AddShopItemRequest request) {
        checkAdmin();
        IntegralShop item = new IntegralShop();
        item.setItemName(request.getItemName());
        item.setItemDesc(request.getItemDesc());
        item.setCostIntegral(request.getCostIntegral());
        item.setItemType(request.getItemType());
        item.setItemValue(request.getItemValue());
        item.setStock(request.getStock());
        item.setEnabled(1);
        item.setSortOrder(request.getSortOrder());
        adminService.addShopItem(item);
        return Result.success();
    }
    @PutMapping("/shop/item/{id}")
    public Result<Void> updateShopItem(@PathVariable Long id, @Valid @RequestBody AddShopItemRequest request) {
        checkAdmin();
        IntegralShop item = new IntegralShop();
        item.setId(id);
        item.setItemName(request.getItemName());
        item.setItemDesc(request.getItemDesc());
        item.setCostIntegral(request.getCostIntegral());
        item.setItemType(request.getItemType());
        item.setItemValue(request.getItemValue());
        item.setStock(request.getStock());
        item.setEnabled(request.getEnabled());
        item.setSortOrder(request.getSortOrder());
        adminService.updateShopItem(item);
        return Result.success();
    }

    private void checkAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new BusinessException(403, "权限不足");
        }
    }
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
    public static class UpdateUserRequest {
        @NotNull(message = "用户ID不能为空")
        private Long id;
        private String nickname;
        private Integer integral;
        private String role;
        private Integer status;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public Integer getIntegral() { return integral; }
        public void setIntegral(Integer integral) { this.integral = integral; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
    public static class GenerateCardRequest {
        @NotNull(message = "生成数量不能为空")
        @Min(value = 1, message = "生成数量必须大于0")
        @Max(1000)
        private Integer count;
        @NotNull(message = "积分值不能为空")
        @Min(value = 1, message = "积分值必须大于0")
        private Integer integral;
        @Size(max = 50, message = "批次号长度不能超过50")
        private String batchNo;
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Integer getIntegral() { return integral; }
        public void setIntegral(Integer integral) { this.integral = integral; }
        public String getBatchNo() { return batchNo; }
        public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    }
    public static class ResetPasswordRequest {
        @NotNull(message = "用户ID不能为空")
        private Long id;
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,32}$",
                 message = "密码必须包含大小写字母和数字，长度8-32位")
        private String password;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    public static class BroadcastRequest {
        @NotBlank(message = "标题不能为空")
        private String title;
        @NotBlank(message = "内容不能为空")
        private String content;
        private String type = "PUSH";
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
    public static class ReplyFeedbackRequest {
        @NotNull(message = "反馈ID不能为空")
        private Long id;
        @NotBlank(message = "回复内容不能为空")
        private String reply;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
    }
    public static class AddShopItemRequest {
        @NotBlank(message = "商品名称不能为空")
        private String itemName;
        private String itemDesc;
        @NotNull(message = "所需积分不能为空")
        private Integer costIntegral;
        @NotBlank(message = "商品类型不能为空")
        private String itemType;
        private String itemValue;
        private Integer stock = -1;
        private Integer sortOrder = 0;
        private Integer enabled = 1;
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getItemDesc() { return itemDesc; }
        public void setItemDesc(String itemDesc) { this.itemDesc = itemDesc; }
        public Integer getCostIntegral() { return costIntegral; }
        public void setCostIntegral(Integer costIntegral) { this.costIntegral = costIntegral; }
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public String getItemValue() { return itemValue; }
        public void setItemValue(String itemValue) { this.itemValue = itemValue; }
        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Integer getEnabled() { return enabled; }
        public void setEnabled(Integer enabled) { this.enabled = enabled; }
    }
}
