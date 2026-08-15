package com.campus.trade.mq.consumer;

import com.campus.trade.config.RabbitConfig;
import com.campus.trade.mq.dto.OrderCreateMessage;
import com.campus.trade.service.impl.OrderServiceImpl;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 下单消息消费者
 * - order.create.queue：异步落库订单（手动 ack，失败重试后进死信）
 * - order.cancel.queue：延迟关单（TTL 到期后进入，状态待接单则取消）
 */
@Component
public class OrderMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderMessageConsumer.class);

    private final OrderServiceImpl orderService;

    public OrderMessageConsumer(OrderServiceImpl orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CREATE_QUEUE)
    public void onCreate(OrderCreateMessage msg, Channel channel, Message message) throws IOException {
        try {
            orderService.persistOrderFromMessage(msg.getOrderNo(), msg.getBuyerId(), msg.getCartIds(), msg.getRemark());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            logger.info("[MQ] 下单消息处理完成并 ack: orderNo={}", msg.getOrderNo());
        } catch (Exception e) {
            logger.error("[MQ] 下单消息处理失败，进入死信队列: orderNo={}", msg.getOrderNo(), e);
            // 业务失败（校验/落库异常）→ 拒绝且不重回队列，进死信兜底
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CANCEL_QUEUE)
    public void onCancelCheck(OrderCreateMessage msg, Channel channel, Message message) throws IOException {
        try {
            orderService.cancelIfPending(msg.getOrderNo());
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 失败不重回队列（避免无限循环），进 cancel 死信队列兜底
            logger.error("[MQ] 关单检查失败，进入死信: orderNo={}", msg.getOrderNo(), e);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }
}
