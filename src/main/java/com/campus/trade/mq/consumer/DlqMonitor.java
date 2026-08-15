package com.campus.trade.mq.consumer;

import com.campus.trade.config.RabbitConfig;
import com.campus.trade.mq.dto.OrderCreateMessage;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 死信队列监控
 * - order.create.dlq：下单失败的消息，记录日志供人工/定时对账补偿
 * - order.cancel.dlq：关单检查失败的消息，同上
 */
@Component
public class DlqMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DlqMonitor.class);

    @RabbitListener(queues = RabbitConfig.ORDER_CREATE_DLQ)
    public void onCreateDlq(OrderCreateMessage msg, Channel channel, Message message) throws IOException {
        // 记录失败消息，ack 防止重复告警；后续可接入告警/重放机制
        logger.error("[DLQ-告警] 下单消息进入死信! orderNo={}, buyerId={}, cartIds={}", 
                msg.getOrderNo(), msg.getBuyerId(), msg.getCartIds());
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CANCEL_DLQ)
    public void onCancelDlq(OrderCreateMessage msg, Channel channel, Message message) throws IOException {
        logger.error("[DLQ-告警] 关单检查消息进入死信! orderNo={}", msg.getOrderNo());
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
