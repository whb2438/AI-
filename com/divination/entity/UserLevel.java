package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_level")
public class UserLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String level;
    private Integer levelPoints;
    private Integer totalDivinationCount;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Integer getLevelPoints() { return levelPoints; }
    public void setLevelPoints(Integer levelPoints) { this.levelPoints = levelPoints; }
    public Integer getTotalDivinationCount() { return totalDivinationCount; }
    public void setTotalDivinationCount(Integer totalDivinationCount) { this.totalDivinationCount = totalDivinationCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
