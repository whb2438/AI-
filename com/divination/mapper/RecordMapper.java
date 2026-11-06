package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.DivinationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecordMapper extends BaseMapper<DivinationRecord> {
    /**
     * Sum total cost_integral across all divination records.
     * Bug fix (BUG-012): replaces full-table Java stream summation.
     */
    @Select("SELECT COALESCE(SUM(cost_integral), 0) FROM divination_record")
    long sumCostIntegral();

    /**
     * Delete all divination records for a user (cascade on user deletion).
     */
    @Delete("DELETE FROM divination_record WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
