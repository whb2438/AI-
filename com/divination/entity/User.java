package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类 - 使用Lombok简化样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private Integer integral;

    private String role;

    private Integer status;

    private LocalDateTime registerTime;

    private LocalDateTime lastLoginTime;

    private String loginIp;

    private Integer loginCount;

    private Integer tokenVersion;

    private String realName;

    private String gender;

    private String birthDate;

    private String birthTime;

    private LocalDateTime vipExpiresAt;

    private String inviteCode; // 用户专属邀请码

    /**
     * 检查VIP状态是否有效
     */
    public boolean isVipActive() {
        return vipExpiresAt != null && vipExpiresAt.isAfter(LocalDateTime.now());
    }
}