package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * Atomically deduct integral. Returns 1 if successful, 0 if balance insufficient.
     * Bug fix: prevents TOCTOU race condition (BUG-003).
     */
    @Update("UPDATE user SET integral = integral - #{amount} WHERE id = #{userId} AND integral >= #{amount}")
    int deductIntegralAtomically(@Param("userId") Long userId, @Param("amount") int amount);

    /**
     * Atomically add integral (always succeeds, even if integral goes negative).
     * Bug fix: prevents TOCTOU race condition (BUG-003).
     */
    @Update("UPDATE user SET integral = integral + #{amount} WHERE id = #{userId}")
    int addIntegralAtomically(@Param("userId") Long userId, @Param("amount") int amount);

    /**
     * Atomically adjust integral with balance guard.
     * Returns 1 if successful, 0 if user doesn't exist or integral would go negative.
     * Bug fix: prevents TOCTOU race condition (BUG-003).
     */
    @Update("UPDATE user SET integral = integral + #{amount} WHERE id = #{userId} AND integral + #{amount} >= 0")
    int adjustIntegralAtomically(@Param("userId") Long userId, @Param("amount") int amount);
}
