package com.campus.trade.mq;

import com.campus.trade.config.RabbitConfig;
import com.campus.trade.mq.dto.OrderCreateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 下单消息生产者
 * - 发送下单消息（异步落库）
 * - 发送延迟关单消息（15 分钟未支付自动取消，TTL + 死信实现）
 * - confirm 回调确认消息到达交换机，returns 回调处理路由失败
 */
@Component
public class OrderCreateProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreateProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderCreateProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        // 发送方确认：confirm 确认已到交换机，returns 处理路由不到队列
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                logger.info("[MQ] 下单消息确认成功: {}", correlationData != null ? correlationData.getId() : "");
            } else {
                logger.error("[MQ] 下单消息确认失败: cause={}", cause);
            }
        });
        this.rabbitTemplate.setReturnsCallback(returned -> logger.error(
                "[MQ] 消息路由失败: msg={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                returned.getMessage(), returned.getReplyCode(), returned.getReplyText(),
                returned.getExchange(), returned.getRoutingKey()));
    }

    /**
     * 发送异步落库下单消息
     */
    public void sendCreate(OrderCreateMessage message) {
        String correlationId = "create:" + message.getOrderNo();
        CorrelationData cd = new CorrelationData(correlationId);
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_CREATE_EXCHANGE,
                RabbitConfig.ORDER_CREATE_QUEUE,
                message,
                cd);
        logger.info("[MQ] 已发送下单消息: orderNo={}", message.getOrderNo());
    }

    /**
     * 发送延迟关单消息（TTL 15min，到期后进入 cancel 队列）
     */
    public void sendDelayCancelCheck(String orderNo, Long buyerId) {
        OrderCreateMessage message = new OrderCreateMessage(
                buyerId, null, null, orderNo, OrderCreateMessage.TYPE_CANCEL_CHECK);
        CorrelationData cd = new CorrelationData("delay:" + orderNo);
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_DELAY_EXCHANGE,
                RabbitConfig.ORDER_DELAY_QUEUE,
                message,
                cd);
        logger.info("[MQ] 已发送延迟关单消息: orderNo={}, 15min后检查", orderNo);
    }
}
