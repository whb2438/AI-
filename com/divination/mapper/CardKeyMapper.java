package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.CardKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CardKeyMapper extends BaseMapper<CardKey> {
    @Select("SELECT * FROM card_key WHERE card_key = #{cardKey} FOR UPDATE")
    CardKey selectForUpdate(@Param("cardKey") String cardKey);

    /**
     * Delete all card_key records for a user (cascade on user deletion).
     */
    @Delete("DELETE FROM card_key WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
