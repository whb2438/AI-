package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.FortuneCalendar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FortuneCalendarMapper extends BaseMapper<FortuneCalendar> {
    @Select("SELECT * FROM fortune_calendar WHERE user_id = #{userId} AND fortune_date BETWEEN #{start} AND #{end} ORDER BY fortune_date")
    List<FortuneCalendar> getFortuneRange(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT * FROM fortune_calendar WHERE user_id = #{userId} AND fortune_date = #{date}")
    FortuneCalendar getByDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
