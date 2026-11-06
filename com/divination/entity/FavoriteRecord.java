package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏记录实体类 - 使用Lombok简化样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("favorite_record")
public class FavoriteRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long recordId;

    private LocalDateTime createTime;
}