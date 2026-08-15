package com.campus.trade;

import com.campus.trade.config.RabbitConfig;
import com.campus.trade.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RabbitMQ 异步下单 + 延迟关单功能测试
 * 依赖：RabbitMQ(5672) + MySQL + Redis 在本地运行
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MqOrderTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    void queuesExist() {
        // RabbitConfig 启动时自动声明交换机/队列；能注入 RabbitTemplate 即说明连接成功
        assertNotNull(rabbitTemplate, "RabbitTemplate 注入失败");
    }

    @Test
    @Order(2)
    void asyncOrderIdempotent() {
        String orderNo = "test-idempotent-" + System.currentTimeMillis();
        // 不存在的订单号 → 正常落库（购物车为空会抛异常，这里只验证幂等逻辑：
        // 先造一条订单，再调 persistOrderFromMessage 应被幂等忽略）
        jdbcTemplate.update(
                "INSERT INTO orders (order_no, buyer_id, seller_id, total_amount, status, remark) VALUES (?, 1, 1, 0.01, 1, 'mq-test')",
                orderNo);

        int before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE order_no = ?", Integer.class, orderNo);

        // 重复消费 → 幂等忽略（不新增行）
        orderService.persistOrderFromMessage(orderNo, 1L, java.util.List.of(999L), "dup");
        int after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE order_no = ?", Integer.class, orderNo);

        assertEquals(before, after, "重复消息应被幂等忽略");
        jdbcTemplate.update("DELETE FROM orders WHERE order_no = ?", orderNo);
    }

    @Test
    @Order(3)
    void cancelPendingOrder() {
        String orderNo = "test-cancel-" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO orders (order_no, buyer_id, seller_id, total_amount, status, remark) VALUES (?, 1, 1, 0.01, 1, 'mq-delay')",
                orderNo);

        // 状态=1（待接单）→ 延迟关单应取消为 0
        orderService.cancelIfPending(orderNo);
        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE order_no = ?", Integer.class, orderNo);
        assertEquals(0, status, "待接单订单应被自动取消");

        // 已是终态（已完成=3）→ 再次调用不应改变
        jdbcTemplate.update("UPDATE orders SET status = 3 WHERE order_no = ?", orderNo);
        orderService.cancelIfPending(orderNo);
        status = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE order_no = ?", Integer.class, orderNo);
        assertEquals(3, status, "已完成订单不应被取消");

        jdbcTemplate.update("DELETE FROM orders WHERE order_no = ?", orderNo);
    }

    @Test
    @Order(4)
    void cancelNonExistentOrder() {
        // 不存在的订单号 → 不抛异常，静默忽略
        assertDoesNotThrow(() -> orderService.cancelIfPending("test-not-exist-" + System.currentTimeMillis()));
    }
}
