package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.IntegralOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IntegralOrderMapper extends BaseMapper<IntegralOrder> {
    @Select("SELECT COALESCE(SUM(cost_integral), 0) FROM integral_order WHERE user_id = #{userId} AND status = 1")
    long sumSpentIntegral(@Param("userId") Long userId);
}
