package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.InviteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InviteRecordMapper extends BaseMapper<InviteRecord> {

    @Select("SELECT COUNT(*) FROM invite_record WHERE inviter_id = #{userId}")
    int getInvitationCount(@Param("userId") Long userId);

    @Select("SELECT SUM(reward_integral) FROM invite_record WHERE inviter_id = #{userId}")
    Integer getTotalRewardIntegral(@Param("userId") Long userId);
}
