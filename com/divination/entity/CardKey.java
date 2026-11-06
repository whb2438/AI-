package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 卡牌密钥实体类 - 使用Lombok简化样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("card_key")
public class CardKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cardKey;

    private Integer integral;

    private String batchNo;

    private Integer status;

    private Long userId;

    private LocalDateTime exchangeTime;

    private LocalDateTime createTime;
}