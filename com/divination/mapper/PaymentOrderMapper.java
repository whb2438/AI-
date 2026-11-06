package com.divination.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.divination.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("SELECT * FROM payment_order WHERE order_no = #{orderNo}")
    PaymentOrder getByOrderNo(@Param("orderNo") String orderNo);

    @Update("UPDATE payment_order SET status = 'EXPIRED' WHERE status = 'PENDING' AND expire_time < NOW()")
    int expireOldOrders();

    @Select("SELECT * FROM payment_order WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<PaymentOrder> getRecentOrders(@Param("userId") Long userId, @Param("limit") int limit);
}
