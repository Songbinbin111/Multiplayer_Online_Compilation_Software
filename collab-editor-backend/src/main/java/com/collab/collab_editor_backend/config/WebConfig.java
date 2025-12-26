package com.collab.collab_editor_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.collab.collab_editor_backend.config.JwtAuthInterceptor;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 1. 新增：定义BCryptPasswordEncoder Bean（解决注入失败问题）
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. CORS跨域配置
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:5173", "http://localhost:5174", "http://localhost:5175") // 允许常用开发端口
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // 3. 添加安全头拦截器，防止XSS攻击
    @Bean
    public HandlerInterceptor securityHeaderInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                // 防止XSS攻击
                response.setHeader("X-XSS-Protection", "1; mode=block");
                // 防止点击劫持
                response.setHeader("X-Frame-Options", "DENY");
                // 防止MIME类型嗅探
                response.setHeader("X-Content-Type-Options", "nosniff");
                // 内容安全策略
                response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");
                return true;
            }
        };
    }

    // 添加JWT认证拦截器
    @Bean
    public JwtAuthInterceptor jwtAuthInterceptor() {
        return new JwtAuthInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeaderInterceptor()).addPathPatterns("/**");
        // 注册JWT认证拦截器，排除不需要认证的路径
        registry.addInterceptor(jwtAuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login")
                .excludePathPatterns("/api/register")
                .excludePathPatterns("/api/refresh-token")
                .excludePathPatterns("/api/user/reset-password/request")
                .excludePathPatterns("/api/user/reset-password")
                .excludePathPatterns("/api/error-logs") // 允许未登录用户上报错误日志
                .excludePathPatterns("/api/monitor/**") // 允许访问监控端点 (仅供测试，生产环境应加权限)
                .excludePathPatterns("/actuator/**"); // 允许访问Actuator端点
    }

    // 配置字符编码过滤器
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }

    @Autowired
    private ObjectMapper objectMapper;

    // 配置消息转换器，确保JSON响应使用UTF-8编码
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 先清空默认的转换器
        converters.clear();
        
        // 创建Jackson消息转换器
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        // 设置UTF-8编码
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(converter);
    }
    
    // 配置静态资源映射，允许访问本地存储的上传文件
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 允许访问/uploads/路径下的文件，映射到本地的uploads/目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}