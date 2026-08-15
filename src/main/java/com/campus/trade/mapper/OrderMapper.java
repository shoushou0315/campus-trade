package com.campus.trade.mapper;

import com.campus.trade.entity.OrderDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface OrderMapper {
    int insert(OrderDO order);
    OrderDO selectById(@Param("id") Long id);
    OrderDO selectByOrderNo(@Param("orderNo") String orderNo);
    List<OrderDO> selectByBuyerId(@Param("buyerId") Long buyerId);
    List<OrderDO> selectBySellerId(@Param("sellerId") Long sellerId);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 查超过 deadline 仍为待接单(1)的订单（定时兜底关单用） */
    List<Map<String, Object>> selectPendingBefore(@Param("deadline") String deadline);
}
