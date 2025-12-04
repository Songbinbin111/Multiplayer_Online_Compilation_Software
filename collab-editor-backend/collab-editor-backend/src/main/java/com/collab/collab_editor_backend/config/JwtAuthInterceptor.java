package com.collab.collab_editor_backend.config;

import com.collab.collab_editor_backend.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器：验证请求头中的Token是否有效
 */
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("=== JWT拦截器开始处理请求 ===");
        log.info("请求URL: {}, 请求方法: {}", request.getRequestURI(), request.getMethod());

        // 1. 放行OPTIONS请求（CORS预检请求）
        if ("OPTIONS".equals(request.getMethod())) {
            log.info("=== OPTIONS请求，直接放行 ===");
            return true;
        }

        // 2. 获取请求头中的Authorization字段
        String authorizationHeader = request.getHeader(jwtUtil.getHeader());
        log.info("=== 接收到的请求头列表 ===");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            log.info("{}: {}", headerName, request.getHeader(headerName));
        });

        // 3. 验证Token是否存在
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            log.error("=== Token不存在 ===");
            throw new RuntimeException("Token不存在，请先登录");
        }

        // 3. 提取纯净Token（去除Bearer前缀和空格）
        log.info("=== 原始Token（含Bearer前缀）==={}", authorizationHeader);
        String token = authorizationHeader.replace("Bearer ", "").trim();
        log.info("=== 去除空格和前缀后的纯净Token ==={}", token);

        try {
            // 4. 调用JwtUtil解析Token（验证签名和有效期）
            log.info("=== Token存在，开始解析验证 ===");
            Claims claims = jwtUtil.parseToken(token);
            log.info("=== Token解析成功 ===");
            log.info("解析出的用户信息：userId={}, username={}",
                    claims.get("userId"), claims.get("username"));

            // 5. 将用户ID存入请求属性（后续接口可直接获取）
            request.setAttribute("userId", claims.get("userId", Long.class));
            return true; // Token有效，放行请求
        } catch (Exception e) {
            log.error("=== Token解析失败 ===");
            log.error("错误原因: {}", e.getMessage());
            throw new RuntimeException("Token无效或已过期：" + e.getMessage());
        }
    }
}