package com.campus.trade;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static String buyerToken;
    private static String sellerToken;
    private static Long orderId;
    private static Long cartId;
    private static Long sellerProductId;
    private static boolean initialized = false;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    @BeforeEach
    void setup() {
        if (sellerToken == null) {
            String seller = "p4s" + System.currentTimeMillis();
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("username", seller);
            body.put("password", "123456");
            body.put("nickname", "P4卖家");
            ResponseEntity<Map> r = restTemplate.postForEntity(url("/api/auth/register"), body, Map.class);
            Map<String, Object> data = (Map<String, Object>) r.getBody().get("data");
            sellerToken = (String) data.get("accessToken");
        }
        if (buyerToken == null) {
            String buyer = "p4b" + System.currentTimeMillis();
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("username", buyer);
            body.put("password", "123456");
            body.put("nickname", "P4买家");
            ResponseEntity<Map> r = restTemplate.postForEntity(url("/api/auth/register"), body, Map.class);
            Map<String, Object> data = (Map<String, Object>) r.getBody().get("data");
            buyerToken = (String) data.get("accessToken");
        }
    }

    @Test
    @Order(1)
    void testSellerPublishProduct() {
        Map<String, Object> product = Map.of(
                "categoryId", 3, "title", "P4测试商品-圆珠笔",
                "description", "测试下单用",
                "price", 9.99, "campus", "信息学部", "condition", 1);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(product, authHeaders(sellerToken));
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/api/products"), entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        sellerProductId = ((Number) data.get("id")).longValue();
        System.out.println("卖家发布商品: id=" + sellerProductId);
    }

    @Test
    @Order(2)
    void testAddToCart() {
        Map<String, Object> body = Map.of("productId", sellerProductId, "quantity", 2);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, authHeaders(buyerToken));
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/api/cart"), entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        System.out.println("买家加入购物车成功");
    }

    @Test
    @Order(3)
    void testCartList() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(buyerToken));
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/cart"), HttpMethod.GET, entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        List<Map> list = (List<Map>) response.getBody().get("data");
        cartId = ((Number) list.get(0).get("id")).longValue();
        System.out.println("购物车: " + list.size() + " 件, cartId=" + cartId);
    }

    @Test
    @Order(4)
    void testCreateOrder() throws InterruptedException {
        Map<String, Object> body = Map.of("cartIds", List.of(cartId.intValue()), "remark", "测试下单-请尽快发货");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, authHeaders(buyerToken));
        ResponseEntity<Map> response = restTemplate.postForEntity(url("/api/orders"), entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        String orderNo = (String) data.get("orderNo");
        assertEquals("PROCESSING", data.get("status"));
        assertNotNull(orderNo);

        // 异步落库：轮询等消费者把订单写入，最多等 5 秒
        Map<String, Object> order = null;
        for (int i = 0; i < 10; i++) {
            HttpEntity<Void> listEntity = new HttpEntity<>(authHeaders(buyerToken));
            ResponseEntity<Map> listResp = restTemplate.exchange(
                    url("/api/orders"), HttpMethod.GET, listEntity, Map.class);
            List<Map> list = (List<Map>) listResp.getBody().get("data");
            order = list.stream()
                    .filter(o -> orderNo.equals(o.get("orderNo")))
                    .findFirst().orElse(null);
            if (order != null) break;
            Thread.sleep(500);
        }
        assertNotNull(order, "异步下单未落库");
        orderId = ((Number) order.get("id")).longValue();
        assertEquals(1, order.get("status"));
    }

    @Test
    @Order(5)
    void testBuyerOrders() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(buyerToken));
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/orders"), HttpMethod.GET, entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        List list = (List) response.getBody().get("data");
        assertFalse(list.isEmpty());
        System.out.println("我买的: " + list.size() + " 笔订单");
    }

    @Test
    @Order(6)
    void testSellerAcceptOrder() {
        String acceptUrl = url("/api/orders/" + orderId + "/status?status=2");
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(sellerToken));
        ResponseEntity<Map> response = restTemplate.exchange(acceptUrl, HttpMethod.PUT, entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        System.out.println("卖家接单成功: 状态 1→2");
    }

    @Test
    @Order(7)
    void testBuyerConfirmComplete() {
        String completeUrl = url("/api/orders/" + orderId + "/status?status=3");
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(buyerToken));
        ResponseEntity<Map> response = restTemplate.exchange(completeUrl, HttpMethod.PUT, entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        System.out.println("买家确认完成: 状态 2→3");
    }

    @Test
    @Order(8)
    void testOrderDetail() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(buyerToken));
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/orders/" + orderId), HttpMethod.GET, entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        Map<String, Object> order = (Map<String, Object>) data.get("order");
        assertEquals(3, order.get("status"));
        assertNotNull(data.get("buyer"));
        assertNotNull(data.get("seller"));
        assertNotNull(data.get("items"));
        System.out.println("订单详情: 状态=已完成, 买家=" + ((Map)data.get("buyer")).get("nickname"));
    }

    @Test
    @Order(9)
    void testSellerOrders() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(sellerToken));
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/orders/sold"), HttpMethod.GET, entity, Map.class);
        assertEquals(200, response.getBody().get("code"));
        List list = (List) response.getBody().get("data");
        assertFalse(list.isEmpty());
        System.out.println("我卖的: " + list.size() + " 笔订单");
    }
}
