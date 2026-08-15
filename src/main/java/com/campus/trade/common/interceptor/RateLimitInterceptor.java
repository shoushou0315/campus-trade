package com.campus.trade.common.interceptor;

import com.campus.trade.common.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, RateLimitEntry> requestMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (annotation == null) {
            return true;
        }

        String ip = getClientIp(request);
        String key = ip + ":" + handlerMethod.getMethod().getName();

        long now = System.currentTimeMillis();
        RateLimitEntry entry = requestMap.get(key);

        if (entry == null || now - entry.startTime > annotation.seconds() * 1000L) {
            requestMap.put(key, new RateLimitEntry(now, 1));
            return true;
        }

        entry.count++;
        if (entry.count > annotation.maxRequests()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过多，请稍后再试\"}");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private static class RateLimitEntry {
        long startTime;
        int count;

        RateLimitEntry(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
