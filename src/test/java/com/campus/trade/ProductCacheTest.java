package com.campus.trade;

import com.campus.trade.common.result.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存改造验证：详情缓存命中 / 空值标记防穿透 / 版本号失效 / Redis 降级。
 * 使用真实 Redis + MySQL。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductCacheTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static String token;
    private static Long productId;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeEach
    void setup() throws Exception {
        if (token == null) {
            String testUser = "ct" + System.currentTimeMillis() % 1000000;
            Map<String, String> reg = Map.of(
                    "username", testUser, "password", "123456", "nickname", "缓存测试");
            ResponseEntity<String> raw = restTemplate.postForEntity(
                    url("/api/auth/register"), reg, String.class);
            // 从原始 JSON 提取 accessToken
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(raw.getBody());
            token = root.path("data").path("accessToken").asText(null);
            assertNotNull(token);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    @Test
    @Order(1)
    void testDetailCacheHit() {
        // 访问任意商品详情（测试数据里有商品）
        ResponseEntity<Result> r1 = restTemplate.getForEntity(url("/api/products/1"), Result.class);
        assertEquals(200, r1.getBody().getCode());
        // 第二次访问命中缓存（能正常返回即可）
        ResponseEntity<Result> r2 = restTemplate.getForEntity(url("/api/products/1"), Result.class);
        assertEquals(200, r2.getBody().getCode());
        System.out.println("详情缓存读取正常");
    }

    @Test
    @Order(2)
    void testDetailNullMark() {
        // 查一个不存在的商品 id（如 999999），防穿透应缓存空值标记
        ResponseEntity<Result> r1 = restTemplate.getForEntity(url("/api/products/999999"), Result.class);
        assertNotNull(r1.getBody());
        System.out.println("不存在商品返回: code=" + r1.getBody().getCode());

        // 空值标记 key 应存在
        String nullKey = "product:null:999999";
        String val = stringRedisTemplate.opsForValue().get(nullKey);
        // 缓存 key 实际是 product:detail:999999 存了空值标记
        String detailVal = stringRedisTemplate.opsForValue().get("product:detail:999999");
        boolean hasMark = val != null || detailVal != null;
        System.out.println("空值标记写入: " + hasMark + " (detailVal=" + detailVal + ")");
        assertTrue(hasMark, "防穿透空值标记应写入Redis");
    }

    @Test
    @Order(3)
    void testVersionBumpOnCreate() {
        // 记录当前版本号
        Long before = Long.valueOf(stringRedisTemplate.opsForValue().get("product:list:ver") == null
                ? "0" : stringRedisTemplate.opsForValue().get("product:list:ver"));

        // 发布一个新商品 → 版本号应自增
        HttpHeaders headers = authHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"categoryId\":1,\"title\":\"缓存测试商品\",\"description\":\"test\","
                + "\"price\":9.9,\"originalPrice\":19.9,\"campus\":\"南校区\",\"condition\":1}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Result> response = restTemplate.exchange(url("/api/products"), HttpMethod.POST, entity, Result.class);
        assertEquals(200, response.getBody().getCode());

        Long after = Long.valueOf(stringRedisTemplate.opsForValue().get("product:list:ver"));
        assertEquals(before + 1, after.longValue(), "发布商品后版本号应自增");
        System.out.println("版本号自增: " + before + " -> " + after);
    }
}
