package com.campus.trade.service;

import com.campus.trade.vo.OrderDetailVO;
import com.campus.trade.vo.OrderVO;

import java.util.List;

public interface OrderService {

    OrderVO create(Long buyerId, List<Long> cartIds, String remark);

    /** 异步下单：同步校验 + 发 MQ + 返回订单号（削峰） */
    String createAsync(Long buyerId, List<Long> cartIds, String remark);

    OrderDetailVO getDetail(Long id);

    List<OrderVO> buyerOrders(Long buyerId);

    List<OrderVO> sellerOrders(Long sellerId);

    void updateStatus(Long id, Long userId, Integer status);
}
