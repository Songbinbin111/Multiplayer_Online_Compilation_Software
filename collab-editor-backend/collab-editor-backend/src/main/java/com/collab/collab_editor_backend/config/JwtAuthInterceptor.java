package com.collab.collab_editor_backend.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import com.collab.collab_editor_backend.util.JwtUtil;

public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    // 构造器注入JwtUtil（确保不为null）
    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始处理
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();
        System.out.println("=== JWT拦截器开始处理请求 ===");
        System.out.println("请求URL: " + requestUri + ", 请求方法: " + requestMethod);

        // 1. 跳过OPTIONS预检请求（避免预检请求被拦截）
        if ("OPTIONS".equals(requestMethod)) {
            System.out.println("=== 检测到OPTIONS预检请求，直接放行 ===");
            return true;
        }

        // 2. 打印所有请求头（关键：验证是否能拿到Authorization）
        System.out.println("=== 接收到的请求头列表 ===");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            System.out.println(headerName + ": " + request.getHeader(headerName));
        });

        // 3. 获取Authorization头（用JwtUtil的getHeader()，与前端一致）
        String jwtHeaderName = jwtUtil.getHeader();
        String token = request.getHeader(jwtHeaderName);
        System.out.println("=== 原始Token（含Bearer前缀） ===" + token);
// 关键修改：去掉"Bearer "前缀（含空格），仅保留纯净Token
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer "共7个字符（B-e-a-r-e-r-空格），截取后7位
        }
        System.out.println("=== 去除空格和前缀后的纯净Token ===" + token);

        // 4. 校验Token是否存在
        if (token == null || token.trim().isEmpty()) {
            System.out.println("=== Token校验失败：请求头中未携带有效Token ===");
            throw new RuntimeException("未携带Token");
        }
        System.out.println("=== Token存在，开始解析验证 ===");

        // 5. 解析Token（失败时打印详细错误，便于排查）
        try {
            Claims claims = jwtUtil.parseToken(token);
            // 记录解析出的关键信息（敏感信息可脱敏）
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            System.out.println("=== Token解析成功 ===");
            System.out.println("解析结果 - userId: " + userId + ", username: " + username);
            System.out.println("Token有效期: " + claims.getExpiration());
        } catch (Exception e) {
            System.out.println("=== Token解析失败 ===");
            System.out.println("错误原因: " + e.getMessage());
            e.printStackTrace(); // 打印堆栈信息，便于定位具体错误（生产环境可根据需要调整）
            throw new RuntimeException("Token无效或已过期：" + e.getMessage());
        }

        System.out.println("=== JWT拦截器处理完毕，请求放行 ===");
        return true;
    }
}