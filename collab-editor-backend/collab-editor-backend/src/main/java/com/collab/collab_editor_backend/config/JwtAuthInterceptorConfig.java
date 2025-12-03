package com.collab.collab_editor_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * JWT拦截器配置：指定拦截哪些接口、放行哪些接口
 */
@Configuration
public class JwtAuthInterceptorConfig implements WebMvcConfigurer {

    /**
     * 将拦截器注册为Bean（让Spring管理，才能注入JwtUtil）
     */
    @Bean
    public JwtAuthInterceptor jwtAuthInterceptor() {
        return new JwtAuthInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor())
                .addPathPatterns("/api/doc/**") // 拦截所有文档相关接口（需要认证）
                .excludePathPatterns(
                        "/api/login",    // 放行登录接口
                        "/api/register"  // 放行注册接口
                );
    }
}