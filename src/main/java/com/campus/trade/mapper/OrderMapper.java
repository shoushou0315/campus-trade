package com.campus.trade.mapper;

import com.campus.trade.entity.OrderDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface OrderMapper {
    int insert(OrderDO order);
    OrderDO selectById(@Param("id") Long id);
    OrderDO selectByOrderNo(@Param("orderNo") String orderNo);
    List<OrderDO> selectByBuyerId(@Param("buyerId") Long buyerId);
    List<OrderDO> selectBySellerId(@Param("sellerId") Long sellerId);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
