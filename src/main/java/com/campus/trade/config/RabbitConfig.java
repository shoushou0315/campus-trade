package com.campus.trade.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * - 下单队列 order.create.queue（异步落库）
 * - 延迟关单：TTL + 死信路由，delay 队列消息超时后进 order.cancel.queue
 * - 发送方 confirm + returns 回调（消息可靠性）
 */
@Configuration
public class RabbitConfig {

    /** 订单超时自动关单时长（毫秒），默认 15 分钟 */
    @Value("${rabbitmq.order-timeout-ms:900000}")
    private int orderTimeoutMs;

    // ===== 下单（异步落库）=====
    public static final String ORDER_CREATE_EXCHANGE = "order.create.exchange";
    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    public static final String ORDER_CREATE_DLX = "order.create.dlx";
    public static final String ORDER_CREATE_DLQ = "order.create.dlq";

    // ===== 延迟关单（TTL + 死信）=====
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_CANCEL_QUEUE = "order.cancel.queue";
    public static final String ORDER_CANCEL_DLX = "order.cancel.dlx";
    public static final String ORDER_CANCEL_DLQ = "order.cancel.dlq";

    // ===== 下单：交换机 + 队列 + 死信 =====
    @Bean
    DirectExchange orderCreateExchange() {
        return new DirectExchange(ORDER_CREATE_EXCHANGE, true, false);
    }

    @Bean
    Queue orderCreateQueue() {
        return QueueBuilder.durable(ORDER_CREATE_QUEUE)
                .deadLetterExchange(ORDER_CREATE_DLX)
                .deadLetterRoutingKey(ORDER_CREATE_DLQ)
                .build();
    }

    @Bean
    Queue orderCreateDlq() {
        return QueueBuilder.durable(ORDER_CREATE_DLQ).build();
    }

    @Bean
    DirectExchange orderCreateDlx() {
        return new DirectExchange(ORDER_CREATE_DLX, true, false);
    }

    @Bean
    Binding orderCreateBinding() {
        return BindingBuilder.bind(orderCreateQueue())
                .to(orderCreateExchange()).with(ORDER_CREATE_QUEUE);
    }

    @Bean
    Binding orderCreateDlqBinding() {
        return BindingBuilder.bind(orderCreateDlq())
                .to(orderCreateDlx()).with(ORDER_CREATE_DLQ);
    }

    // ===== 延迟关单：delay 队列（TTL）→ 死信 → cancel 队列 =====
    @Bean
    DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    @Bean
    Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .ttl(orderTimeoutMs)                 // 超时自动关单（可配置）
                .deadLetterExchange(ORDER_DELAY_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CANCEL_QUEUE)
                .build();
    }

    @Bean
    Queue orderCancelQueue() {
        return QueueBuilder.durable(ORDER_CANCEL_QUEUE)
                .deadLetterExchange(ORDER_CANCEL_DLX)
                .deadLetterRoutingKey(ORDER_CANCEL_DLQ)
                .build();
    }

    @Bean
    Queue orderCancelDlq() {
        return QueueBuilder.durable(ORDER_CANCEL_DLQ).build();
    }

    @Bean
    DirectExchange orderCancelDlx() {
        return new DirectExchange(ORDER_CANCEL_DLX, true, false);
    }

    @Bean
    Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange()).with(ORDER_DELAY_QUEUE);
    }

    @Bean
    Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue())
                .to(orderDelayExchange()).with(ORDER_CANCEL_QUEUE);
    }

    @Bean
    Binding orderCancelDlqBinding() {
        return BindingBuilder.bind(orderCancelDlq())
                .to(orderCancelDlx()).with(ORDER_CANCEL_DLQ);
    }

    // ===== 消息转换 + 发送方确认 =====
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setMandatory(true);
        return template;
    }

    // ===== 监听容器工厂：消费者也用 JSON 转换器（与生产者一致）=====
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        return factory;
    }
}
