package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分订单实体类 - 使用Lombok简化样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("integral_order")
public class IntegralOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long shopItemId;

    private Integer costIntegral;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;
}