package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.SignInRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;

@Mapper
public interface SignInRecordMapper extends BaseMapper<SignInRecord> {
    @Select("SELECT COUNT(*) FROM sign_in_record WHERE user_id = #{userId} AND sign_in_date >= #{startDate}")
    int countRecentSignIn(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    @Select("SELECT COALESCE(MAX(consecutive_days), 0) FROM sign_in_record WHERE user_id = #{userId}")
    int getMaxConsecutiveDays(@Param("userId") Long userId);
}
