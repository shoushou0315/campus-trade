package com.campus.trade;

import com.campus.trade.common.result.Result;
import com.campus.trade.dto.request.UserLoginDTO;
import com.campus.trade.dto.request.UserRegisterDTO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private static String token;
    private static String refreshToken;
    private static final String TEST_USERNAME = "t2_" + System.currentTimeMillis();
    private static final String TEST_PASSWORD = "123456";

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @Order(1)
    void testRegister() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword(TEST_PASSWORD);
        dto.setNickname("P2测试用户");
        dto.setPhone("13900000001");
        dto.setSchool("武汉大学");

        ResponseEntity<Result> response = restTemplate.postForEntity(
                url("/api/auth/register"), dto, Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());

        System.out.println("注册成功: " + TEST_USERNAME);
    }

    @Test
    @Order(2)
    void testRegisterDuplicate() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword("654321");
        dto.setNickname("重复用户");

        ResponseEntity<Result> response = restTemplate.postForEntity(
                url("/api/auth/register"), dto, Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        System.out.println("重复注册被拦截: " + response.getBody().getMessage());
    }

    @Test
    @Order(3)
    void testLogin() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword(TEST_PASSWORD);

        ResponseEntity<Result> response = restTemplate.postForEntity(
                url("/api/auth/login"), dto, Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());

        java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getBody().getData();
        assertNotNull(data);
        token = (String) data.get("accessToken");
        refreshToken = (String) data.get("refreshToken");
        assertNotNull(token);
        assertNotNull(refreshToken);

        System.out.println("登录成功, accessToken: " + token.substring(0, 30) + "...");
    }

    @Test
    @Order(4)
    void testLoginWrongPassword() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword("wrongpass");

        ResponseEntity<Result> response = restTemplate.postForEntity(
                url("/api/auth/login"), dto, Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
        System.out.println("错误密码被拦截: " + response.getBody().getMessage());
    }

    @Test
    @Order(5)
    void testGetUserInfoWithToken() {
        assertNotNull(token, "需要先登录获取token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Result> response = restTemplate.exchange(
                url("/api/user/info"), HttpMethod.GET, entity, Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());

        java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getBody().getData();
        assertNotNull(data);
        assertEquals(TEST_USERNAME, data.get("username"));
        System.out.println("Token验证通过, 获取用户信息: " + data.get("nickname"));
    }

    @Test
    @Order(6)
    void testAccessWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/api/user/info"), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        System.out.println("无Token访问被拦截: 403 Forbidden");
    }

    @Test
    @Order(7)
    void testRefreshToken() {
        assertNotNull(refreshToken, "需要先登录获取refreshToken");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("refreshToken", refreshToken), headers);

        ResponseEntity<Result> response = restTemplate.exchange(
                url("/api/auth/refresh"), HttpMethod.POST, entity, Result.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());

        java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.getBody().getData();
        assertNotNull(data);
        String newAccess = (String) data.get("accessToken");
        String newRefresh = (String) data.get("refreshToken");
        assertNotNull(newAccess);
        assertNotNull(newRefresh);

        // 旧 refresh 已轮换失效，再次使用应报错
        ResponseEntity<Result> response2 = restTemplate.exchange(
                url("/api/auth/refresh"), HttpMethod.POST, entity, Result.class);
        assertEquals(400, response2.getBody().getCode());

        // 更新为新的 token 供后续测试使用
        token = newAccess;
        refreshToken = newRefresh;
        System.out.println("刷新token成功，旧refresh已轮换失效");
    }

    @Test
    @Order(8)
    void testLogoutInvalidatesToken() {
        assertNotNull(token, "需要先登录获取token");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 登出
        ResponseEntity<Result> logoutResp = restTemplate.exchange(
                url("/api/auth/logout"), HttpMethod.POST, entity, Result.class);
        assertEquals(HttpStatus.OK, logoutResp.getStatusCode());

        // 登出后旧 token 应失效（Redis 白名单已删）
        ResponseEntity<Result> infoResp = restTemplate.exchange(
                url("/api/user/info"), HttpMethod.GET, entity, Result.class);
        assertEquals(HttpStatus.UNAUTHORIZED, infoResp.getStatusCode());
        System.out.println("登出后token立即失效: 403");
    }

    @Test
    @Order(9)
    void testSingleDeviceLogin() {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(TEST_USERNAME);
        dto.setPassword(TEST_PASSWORD);

        // 第一次登录拿 tokenA
        ResponseEntity<Result> login1 = restTemplate.postForEntity(url("/api/auth/login"), dto, Result.class);
        java.util.Map<String, Object> data1 = (java.util.Map<String, Object>) login1.getBody().getData();
        String tokenA = (String) data1.get("accessToken");

        // 第二次登录拿 tokenB（覆盖 Redis 白名单）
        ResponseEntity<Result> login2 = restTemplate.postForEntity(url("/api/auth/login"), dto, Result.class);
        java.util.Map<String, Object> data2 = (java.util.Map<String, Object>) login2.getBody().getData();
        String tokenB = (String) data2.get("accessToken");

        // tokenA 应失效（被踢下线），tokenB 有效
        HttpHeaders headersA = new HttpHeaders();
        headersA.set("Authorization", "Bearer " + tokenA);
        HttpEntity<Void> entityA = new HttpEntity<>(headersA);
        ResponseEntity<Result> respA = restTemplate.exchange(url("/api/user/info"), HttpMethod.GET, entityA, Result.class);

        HttpHeaders headersB = new HttpHeaders();
        headersB.set("Authorization", "Bearer " + tokenB);
        HttpEntity<Void> entityB = new HttpEntity<>(headersB);
        ResponseEntity<Result> respB = restTemplate.exchange(url("/api/user/info"), HttpMethod.GET, entityB, Result.class);

        assertEquals(HttpStatus.UNAUTHORIZED, respA.getStatusCode());
        assertEquals(HttpStatus.OK, respB.getStatusCode());
        System.out.println("单设备登录生效: 旧token被踢下线，新token有效");
    }
}
