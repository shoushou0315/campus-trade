package com.campus.trade.service.impl;

import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.entity.*;
import com.campus.trade.mapper.*;
import com.campus.trade.service.OrderService;
import com.campus.trade.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            CartMapper cartMapper, ProductMapper productMapper, UserMapper userMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public OrderVO create(Long buyerId, List<Long> cartIds, String remark) {
        List<CartDO> carts = cartMapper.selectByIds(cartIds);
        if (carts.isEmpty()) {
            throw new BusinessException("购物车为空");
        }

        Long sellerId = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItemDO> items = new ArrayList<>();

        for (CartDO cart : carts) {
            ProductDO product = productMapper.selectById(cart.getProductId());
            if (product == null || product.getStatus() != 1) {
                throw new BusinessException("商品【" + product.getTitle() + "】已下架");
            }
            if (sellerId == null) {
                sellerId = product.getSellerId();
            } else if (!sellerId.equals(product.getSellerId())) {
                throw new BusinessException("不同卖家商品需分开下单");
            }

            OrderItemDO item = new OrderItemDO();
            item.setProductId(product.getId());
            item.setProductTitle(product.getTitle());
            item.setProductImage(product.getImages());
            item.setPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            items.add(item);

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        OrderDO order = new OrderDO();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setTotalAmount(totalAmount);
        order.setStatus(1);
        order.setRemark(remark);
        orderMapper.insert(order);

        for (OrderItemDO item : items) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.insertBatch(items);

        // 清空已下单的购物车项
        for (Long cartId : cartIds) {
            cartMapper.deleteById(cartId);
        }

        logger.info("用户{}下单成功, 订单号: {}, 金额: {}", buyerId, order.getOrderNo(), totalAmount);
        return toVO(order);
    }

    @Override
    public OrderDetailVO getDetail(Long id) {
        OrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        List<OrderItemDO> items = orderItemMapper.selectByOrderId(id);
        UserDO buyer = userMapper.selectById(order.getBuyerId());
        UserDO seller = userMapper.selectById(order.getSellerId());

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(toVO(order));
        vo.setBuyer(buyer != null ? UserVO.from(buyer) : null);
        vo.setSeller(seller != null ? UserVO.from(seller) : null);
        vo.setItems(items.stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public List<OrderVO> buyerOrders(Long buyerId) {
        return orderMapper.selectByBuyerId(buyerId).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<OrderVO> sellerOrders(Long sellerId) {
        return orderMapper.selectBySellerId(sellerId).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void updateStatus(Long id, Long userId, Integer status) {
        OrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (status == 0) {
            if (!order.getBuyerId().equals(userId)) {
                throw new BusinessException("只能取消自己的订单");
            }
            if (order.getStatus() != 1) {
                throw new BusinessException("只能取消待接单的订单");
            }
        } else if (status == 2) {
            if (!order.getSellerId().equals(userId)) {
                throw new BusinessException("只能接自己的订单");
            }
            if (order.getStatus() != 1) {
                throw new BusinessException("只能接待接单的订单");
            }
        } else if (status == 3) {
            if (!order.getBuyerId().equals(userId)) {
                throw new BusinessException("只能确认自己的订单");
            }
            if (order.getStatus() != 2) {
                throw new BusinessException("只能确认已接单的订单");
            }
        }

        orderMapper.updateStatus(id, status);
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return date + uuid;
    }

    private OrderVO toVO(OrderDO o) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setBuyerId(o.getBuyerId());
        vo.setSellerId(o.getSellerId());
        vo.setTotalAmount(o.getTotalAmount());
        vo.setStatus(o.getStatus());
        vo.setRemark(o.getRemark());
        vo.setGmtCreate(o.getGmtCreate());
        return vo;
    }

    private OrderItemVO toItemVO(OrderItemDO i) {
        OrderItemVO vo = new OrderItemVO();
        vo.setProductId(i.getProductId());
        vo.setProductTitle(i.getProductTitle());
        vo.setProductImage(i.getProductImage());
        vo.setPrice(i.getPrice());
        vo.setQuantity(i.getQuantity());
        return vo;
    }
}
