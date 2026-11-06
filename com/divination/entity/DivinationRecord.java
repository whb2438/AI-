package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测算记录实体类 - 使用Lombok简化样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("divination_record")
public class DivinationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type;

    private String inputInfo;

    private String result;

    private Integer costIntegral;

    private LocalDateTime createTime;

    /**
     * 命盘结构化数据（JSON）：四柱、五行分布、十神、日主强弱、大运、流年等。
     * 由 BaziCalculator 本地排盘生成，供结果页直接渲染「命盘卡」。
     * 旧记录此列为 NULL，前端做空值降级。
     */
    @TableField("chart_data")
    private String chartData;
}