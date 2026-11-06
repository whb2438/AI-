package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分商城商品实体类 - 使用Lombok简化样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("integral_shop")
public class IntegralShop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemName;

    private String itemDesc;

    private Integer costIntegral;

    private String itemType;

    private String itemValue;

    private Integer stock;

    private Integer enabled;

    private Integer sortOrder;

    private LocalDateTime createTime;
}