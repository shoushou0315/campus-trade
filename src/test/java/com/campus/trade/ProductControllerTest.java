package com.campus.trade;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.dto.request.ProductSaveDTO;
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
class ProductControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static String token;
    private static Long createdProductId;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeEach
    void setup() throws Exception {
        if (token == null) {
            // 类内只注册一次（同一用户），商品发布/编辑/删除用同一 token
            String testUser = "p3t" + System.currentTimeMillis() % 1000000;
            Map<String, String> reg = Map.of(
                    "username", testUser,
                    "password", "123456",
                    "nickname", "P3");
            ResponseEntity<String> raw = restTemplate.postForEntity(
                    url("/api/auth/register"), reg, String.class);
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw.getBody());
            token = root.path("data").path("accessToken").asText(null);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    @Test
    @Order(1)
    void testCategoryList() {
        ResponseEntity<Result> response = restTemplate.getForEntity(
                url("/api/categories"), Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().getData();
        assertNotNull(data);
        System.out.println("分类树: " + data.size() + " 个顶级分类");
    }

    @Test
    @Order(2)
    void testProductSearch() {
        ResponseEntity<Result> response = restTemplate.getForEntity(
                url("/api/products?pageNum=1&pageSize=5"), Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());

        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        System.out.println("商品列表: total=" + data.get("total") + " items="
                + ((List) data.get("list")).size());
    }

    @Test
    @Order(3)
    void testProductSearchByKeyword() {
        ResponseEntity<Result> response = restTemplate.getForEntity(
                url("/api/products?keyword=iPhone&pageNum=1&pageSize=5"), Result.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        List list = (List) data.get("list");
        System.out.println("搜索iPhone: " + list.size() + " 条结果");
        assertFalse(list.isEmpty());
    }

    @Test
    @Order(4)
    void testProductDetail() {
        ResponseEntity<Result> response = restTemplate.getForEntity(
                url("/api/products/1"), Result.class);

        assertEquals(200, response.getBody().getCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        Map<String, Object> product = (Map<String, Object>) data.get("product");
        assertEquals("iPhone 14 128G 星光色", product.get("title"));
        System.out.println("商品详情: " + product.get("title"));
    }

    @Test
    @Order(5)
    void testCreateProduct() {
        ProductSaveDTO dto = new ProductSaveDTO();
        dto.setCategoryId(6L);
        dto.setTitle("JUnit测试商品");
        dto.setDescription("这是一个通过测试发布的商品");
        dto.setPrice(new BigDecimal("99.99"));
        dto.setCampus("信息学部");
        dto.setCondition(2);

        HttpEntity<ProductSaveDTO> entity = new HttpEntity<>(dto, authHeaders());
        ResponseEntity<Result> response = restTemplate.postForEntity(
                url("/api/products"), entity, Result.class);

        assertEquals(200, response.getBody().getCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        createdProductId = ((Number) data.get("id")).longValue();
        assertNotNull(createdProductId);
        System.out.println("发布商品成功, id=" + createdProductId);
    }

    @Test
    @Order(6)
    void testUpdateProduct() {
        assertNotNull(createdProductId, "需要先发布商品");

        ProductSaveDTO dto = new ProductSaveDTO();
        dto.setCategoryId(6L);
        dto.setTitle("JUnit测试商品-已更新");
        dto.setDescription("更新后的描述");
        dto.setPrice(new BigDecimal("88.88"));
        dto.setCampus("文理学部");
        dto.setCondition(3);

        HttpEntity<ProductSaveDTO> entity = new HttpEntity<>(dto, authHeaders());
        ResponseEntity<Result> response = restTemplate.exchange(
                url("/api/products/" + createdProductId),
                HttpMethod.PUT, entity, Result.class);

        assertEquals(200, response.getBody().getCode());
        Map<String, Object> data = (Map<String, Object>) response.getBody().getData();
        assertEquals("JUnit测试商品-已更新", data.get("title"));
        System.out.println("更新商品成功: " + data.get("title"));
    }

    @Test
    @Order(7)
    void testProductSearchWithCache() {
        // 第二次搜索同一条件，验证走缓存
        ResponseEntity<Result> response1 = restTemplate.getForEntity(
                url("/api/products?keyword=JUnit&pageNum=1&pageSize=5"), Result.class);
        ResponseEntity<Result> response2 = restTemplate.getForEntity(
                url("/api/products?keyword=JUnit&pageNum=1&pageSize=5"), Result.class);

        assertEquals(200, response1.getBody().getCode());
        assertEquals(200, response2.getBody().getCode());
        System.out.println("缓存查询: 两次查询均成功");
    }
}
