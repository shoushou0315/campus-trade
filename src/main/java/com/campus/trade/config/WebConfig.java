package com.campus.trade.config;

import com.campus.trade.common.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 限流开关（测试环境可关闭，避免测试互相干扰） */
    private final boolean rateLimitEnabled;

    public WebConfig(@Value("${app.rate-limit.enabled:true}") boolean rateLimitEnabled) {
        this.rateLimitEnabled = rateLimitEnabled;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!rateLimitEnabled) {
            return;
        }
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/auth/**");
    }
}
