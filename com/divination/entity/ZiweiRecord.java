package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ziwei_record")
public class ZiweiRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String birthDate;       // 农历日期
    private String birthTimeZhi;    // 时辰地支
    private String gender;
    private String palaceData;      // JSON
    private String result;
    private Integer costIntegral;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getBirthTimeZhi() { return birthTimeZhi; }
    public void setBirthTimeZhi(String birthTimeZhi) { this.birthTimeZhi = birthTimeZhi; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPalaceData() { return palaceData; }
    public void setPalaceData(String palaceData) { this.palaceData = palaceData; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Integer getCostIntegral() { return costIntegral; }
    public void setCostIntegral(Integer costIntegral) { this.costIntegral = costIntegral; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
