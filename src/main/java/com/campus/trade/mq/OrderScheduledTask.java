package com.campus.trade.mq;

import com.campus.trade.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 订单超时兜底（MQ 延迟关单的双保险）
 * - 定时扫描超时未支付的待接单(1)订单，自动取消为(0)
 * - 即使 RabbitMQ 消息丢失/宕机，订单仍能被取消
 */
@Component
public class OrderScheduledTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderScheduledTask.class);

    private final OrderMapper orderMapper;

    /** 订单超时分钟数（与 MQ TTL 一致），默认 15 分钟 */
    @Value("${rabbitmq.order-timeout-minutes:15}")
    private int timeoutMinutes;

    public OrderScheduledTask(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /** 每 5 分钟扫一次超时订单 */
    @Scheduled(cron = "${rabbitmq.timeout-scan-cron:0 */5 * * * ?}")
    public void cancelTimeoutOrders() {
        // 超过 timeoutMinutes 分钟、状态仍为待接单(1) 的订单
        String deadline = LocalDateTime.now()
                .minusMinutes(timeoutMinutes)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        try {
            List<Map<String, Object>> timeoutOrders = orderMapper.selectPendingBefore(deadline);
            if (timeoutOrders.isEmpty()) {
                return;
            }
            for (Map<String, Object> order : timeoutOrders) {
                Long id = ((Number) order.get("id")).longValue();
                orderMapper.updateStatus(id, 0);
                logger.info("[定时兜底] 超时订单自动取消: id={}, orderNo={}", id, order.get("order_no"));
            }
            logger.info("[定时兜底] 本次取消 {} 笔超时订单", timeoutOrders.size());
        } catch (Exception e) {
            logger.error("[定时兜底] 扫描超时订单失败: {}", e.getMessage());
        }
    }
}
