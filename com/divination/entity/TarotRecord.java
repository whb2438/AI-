package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("tarot_record")
public class TarotRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String spreadType;     // THREE_CARD / SEVEN_CARD / TEN_CARD
    private String cardsDrawn;     // JSON array of card IDs
    private String result;
    private Integer costIntegral;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSpreadType() { return spreadType; }
    public void setSpreadType(String spreadType) { this.spreadType = spreadType; }
    public String getCardsDrawn() { return cardsDrawn; }
    public void setCardsDrawn(String cardsDrawn) { this.cardsDrawn = cardsDrawn; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Integer getCostIntegral() { return costIntegral; }
    public void setCostIntegral(Integer costIntegral) { this.costIntegral = costIntegral; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
