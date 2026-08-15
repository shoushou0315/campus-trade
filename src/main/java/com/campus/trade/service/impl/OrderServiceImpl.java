package com.campus.trade.service.impl;

import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.entity.*;
import com.campus.trade.enums.OrderStatusEnum;
import com.campus.trade.enums.ProductStatusEnum;
import com.campus.trade.mapper.*;
import com.campus.trade.mq.OrderCreateProducer;
import com.campus.trade.mq.dto.OrderCreateMessage;
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
    private final OrderCreateProducer orderCreateProducer;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            CartMapper cartMapper, ProductMapper productMapper,
                            UserMapper userMapper, OrderCreateProducer orderCreateProducer) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.orderCreateProducer = orderCreateProducer;
    }

    /**
     * 同步下单（保留原链路，测试/兜底用）
     */
    @Override
    @Transactional
    public OrderVO create(Long buyerId, List<Long> cartIds, String remark) {
        OrderDO order = persistOrder(buyerId, cartIds, remark, generateOrderNo());
        return toVO(order);
    }

    /**
     * 异步下单：同步校验（防无效下单）+ 发 MQ + 立即返回订单号
     * 削峰：DB 落库由消费者异步执行，接口不阻塞
     */
    @Override
    public String createAsync(Long buyerId, List<Long> cartIds, String remark) {
        // 同步校验：购物车非空、商品在售、同卖家（失败直接抛异常，不发消息）
        List<CartDO> carts = cartMapper.selectByIds(cartIds);
        if (carts.isEmpty()) {
            throw new BusinessException("购物车为空");
        }
        validateCarts(carts);

        String orderNo = generateOrderNo();
        OrderCreateMessage message = new OrderCreateMessage(
                buyerId, cartIds, remark, orderNo, OrderCreateMessage.TYPE_CREATE);
        orderCreateProducer.sendCreate(message);
        // 同时发送延迟关单检查（15 分钟未支付自动取消）
        orderCreateProducer.sendDelayCancelCheck(orderNo, buyerId);

        logger.info("用户{}异步下单已受理, 订单号: {}", buyerId, orderNo);
        return orderNo;
    }

    /**
     * 消费者调用：幂等落库下单（事务）
     * 重复消息（order_no 已存在）直接忽略
     */
    @Transactional
    public void persistOrderFromMessage(String orderNo, Long buyerId, List<Long> cartIds, String remark) {
        if (orderMapper.selectByOrderNo(orderNo) != null) {
            logger.info("[MQ] 订单已存在，幂等忽略: orderNo={}", orderNo);
            return;
        }
        OrderDO order = persistOrder(buyerId, cartIds, remark, orderNo);
        logger.info("[MQ] 异步下单成功, 订单号: {}, 金额: {}", order.getOrderNo(), order.getTotalAmount());
    }

    /** 延迟关单：状态仍为待接单(1)则取消，否则忽略 */
    @Transactional
    public void cancelIfPending(String orderNo) {
        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            logger.info("[MQ] 关单检查：订单不存在，忽略 orderNo={}", orderNo);
            return;
        }
        if (order.getStatus() == OrderStatusEnum.PENDING.getCode()) {
            orderMapper.updateStatus(order.getId(), OrderStatusEnum.CANCELED.getCode());
            logger.info("[MQ] 订单超时自动取消: orderNo={}, id={}", orderNo, order.getId());
        } else {
            logger.info("[MQ] 关单检查：订单状态={}，无需取消 orderNo={}", order.getStatus(), orderNo);
        }
    }

    /** 校验购物车商品（在售 + 同卖家），返回明细列表 */
    private void validateCarts(List<CartDO> carts) {
        Long sellerId = null;
        for (CartDO cart : carts) {
            ProductDO product = productMapper.selectById(cart.getProductId());
            if (product == null || product.getStatus() != ProductStatusEnum.ON_SALE.getCode()) {
                throw new BusinessException("商品【" + (product != null ? product.getTitle() : "未知") + "】已下架");
            }
            if (sellerId == null) {
                sellerId = product.getSellerId();
            } else if (!sellerId.equals(product.getSellerId())) {
                throw new BusinessException("不同卖家商品需分开下单");
            }
        }
    }

    /** 事务落库：插订单 + 批量明细 + 清购物车（原 create 核心逻辑） */
    private OrderDO persistOrder(Long buyerId, List<Long> cartIds, String remark, String orderNo) {
        List<CartDO> carts = cartMapper.selectByIds(cartIds);
        if (carts.isEmpty()) {
            throw new BusinessException("购物车为空");
        }

        Long sellerId = null;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItemDO> items = new ArrayList<>();

        for (CartDO cart : carts) {
            ProductDO product = productMapper.selectById(cart.getProductId());
            if (product == null || product.getStatus() != ProductStatusEnum.ON_SALE.getCode()) {
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
        order.setOrderNo(orderNo);
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatusEnum.PENDING.getCode());
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
        return order;
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

        if (status == OrderStatusEnum.CANCELED.getCode()) {
            if (!order.getBuyerId().equals(userId)) {
                throw new BusinessException("只能取消自己的订单");
            }
            if (order.getStatus() != OrderStatusEnum.PENDING.getCode()) {
                throw new BusinessException("只能取消待接单的订单");
            }
        } else if (status == OrderStatusEnum.ACCEPTED.getCode()) {
            if (!order.getSellerId().equals(userId)) {
                throw new BusinessException("只能接自己的订单");
            }
            if (order.getStatus() != OrderStatusEnum.PENDING.getCode()) {
                throw new BusinessException("只能接待接单的订单");
            }
        } else if (status == OrderStatusEnum.COMPLETED.getCode()) {
            if (!order.getBuyerId().equals(userId)) {
                throw new BusinessException("只能确认自己的订单");
            }
            if (order.getStatus() != OrderStatusEnum.ACCEPTED.getCode()) {
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
