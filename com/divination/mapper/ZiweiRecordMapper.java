package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.ZiweiRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ZiweiRecordMapper extends BaseMapper<ZiweiRecord> {

    /**
     * 删除用户的所有紫微测算记录（用户删除级联）。
     */
    @Delete("DELETE FROM ziwei_record WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
