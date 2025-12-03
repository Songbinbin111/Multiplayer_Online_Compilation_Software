package com.collab.collab_editor_backend.controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import com.collab.collab_editor_backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器：登录接口（生成 JWT 令牌）
 */

@RestController
public class UserController {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 登录接口（无需令牌，公开访问）
     * @param username 用户名（模拟输入，实际需查数据库验证）
     * @param password 密码（模拟输入，实际需加密验证）
     * @return 包含 JWT 令牌的结果
     */
    @PostMapping("/api/login")
    public Map<String, Object> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        // 1. 模拟数据库验证（实际项目需替换为：查询数据库 -> 比对加密后的密码）
        // 这里简化：假设用户名=admin，密码=123456 为合法用户
        if (!"admin".equals(username) || !"123456".equals(password)) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("code", 400);
            errorResult.put("message", "用户名或密码错误");
            return errorResult;
        }

        // 2. 验证通过，生成 JWT 令牌（传入用户ID=1，用户名=admin，实际需从数据库获取）
        String token = jwtUtil.generateToken(1L, username);

        // 3. 返回令牌给前端（前端需存储令牌，后续请求在 Header 中携带）
        Map<String, Object> successResult = new HashMap<>();
        successResult.put("code", 200);
        successResult.put("message", "登录成功");
        successResult.put("token", token); // 令牌，前端需保存（如 localStorage）
        successResult.put("username", username);
        return successResult;
    }

    /**
     * 测试需要认证的接口（/api/** 开头，会被拦截器验证令牌）
     */
    @PostMapping("/api/test/auth")
    public Map<String, Object> testAuth() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "接口访问成功（已通过 JWT 认证）");
        return result;
    }
}