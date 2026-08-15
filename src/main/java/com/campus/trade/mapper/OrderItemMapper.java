package com.campus.trade.mapper;

import com.campus.trade.entity.OrderItemDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface OrderItemMapper {
    int insertBatch(@Param("items") List<OrderItemDO> items);
    List<OrderItemDO> selectByOrderId(@Param("orderId") Long orderId);
}
