package com.collab.collab_editor_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.collab.collab_editor_backend.util.JwtUtil;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtUtil jwtUtil;

    // 注册拦截器（确保JwtUtil注入成功，避免空指针）
    @Bean
    public JwtAuthInterceptor jwtAuthInterceptor() {
        return new JwtAuthInterceptor(jwtUtil);
    }

    /**
     * 核心配置：保留allowedOriginPatterns("*")，显式放行Authorization头
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // 保留*，确保登录成功
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 包含预检方法
                // 关键1：显式允许Authorization头，避免被CORS过滤器过滤
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                // 关键2：暴露Authorization头，确保前端能读取响应头（可选，但增强兼容性）
                .exposedHeaders("Authorization")
                .allowCredentials(true) // 保留携带凭证
                .maxAge(3600); // 预检请求缓存1小时，减少重复验证
    }

    /**
     * 拦截器配置：保持不变，排除公开接口
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login")
                .excludePathPatterns("/upload")
                .excludePathPatterns("/ws/**");
    }
}