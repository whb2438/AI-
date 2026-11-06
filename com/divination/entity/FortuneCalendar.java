package com.divination.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运势日历实体类 - 使用Lombok简化样板代码
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fortune_calendar")
public class FortuneCalendar {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate fortuneDate;

    private Integer overallScore;

    private Integer careerScore;

    private Integer wealthScore;

    private Integer loveScore;

    private Integer healthScore;

    private String fortuneText;

    private String luckyColor;

    private String luckyNumber;

    private String luckyDirection;

    private LocalDateTime createTime;
}