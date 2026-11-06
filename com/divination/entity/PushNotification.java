package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("push_notification")
public class PushNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer enableFortune;
    private Integer enablePromotion;
    private LocalDateTime enabledAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getEnableFortune() { return enableFortune; }
    public void setEnableFortune(Integer enableFortune) { this.enableFortune = enableFortune; }
    public Integer getEnablePromotion() { return enablePromotion; }
    public void setEnablePromotion(Integer enablePromotion) { this.enablePromotion = enablePromotion; }
    public LocalDateTime getEnabledAt() { return enabledAt; }
    public void setEnabledAt(LocalDateTime enabledAt) { this.enabledAt = enabledAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
