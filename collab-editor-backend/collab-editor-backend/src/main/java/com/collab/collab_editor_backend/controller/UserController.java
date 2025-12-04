package com.collab.collab_editor_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController {
    // 添加日志记录器
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    // 注入用户Mapper（操作数据库t_user表）
    @Autowired
    private UserMapper userMapper;

    // 注入JWT工具类（生成Token）
    @Autowired
    private JwtUtil jwtUtil;

    // 注入密码加密工具（Spring Security提供，已在WebConfig中配置Bean）
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 注册接口（/api/register）- 补全实现
     */
    @PostMapping("/api/register")
    public Result register(@RequestBody User user) {
        logger.info("收到注册请求，用户名: {}", user.getUsername());
        
        // 1. 校验参数（用户名、密码不能为空）
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            logger.error("注册失败：用户名不能为空");
            return Result.error("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            logger.error("注册失败：密码不能为空");
            return Result.error("密码不能为空");
        }

        // 2. 校验用户名是否已存在（查询数据库）
        User existingUser = userMapper.selectByUsername(user.getUsername());
        if (existingUser != null) {
            logger.error("注册失败：用户名 {} 已被占用", user.getUsername());
            return Result.error("用户名已被占用，请更换");
        }

        // 3. 密码加密（BCrypt算法，不可逆）
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);
        
        // 4. 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        // 5. 保存用户到数据库（MyBatis-Plus的insert方法）
        int insert = userMapper.insert(user);
        if (insert > 0) {
            logger.info("注册成功：用户名 {}", user.getUsername());
            return Result.successWithMessage("注册成功，请登录");
        } else {
            logger.error("注册失败：数据库插入失败，用户名 {}", user.getUsername());
            return Result.error("注册失败，请重试");
        }
    }

    /**
     * 登录接口（/api/login）- 修正参数接收和逻辑
     */
    @PostMapping("/api/login")
    public Result login(@RequestBody User user) {
        // 1. 校验参数
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("用户名或密码不能为空");
        }

        // 2. 查询数据库中的用户
        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser == null) {
            return Result.error("用户名或密码错误");
        }

        // 3. 比对密码（加密后的密码无法解密，用matches方法校验）
        boolean passwordMatch = passwordEncoder.matches(user.getPassword(), dbUser.getPassword());
        if (!passwordMatch) {
            return Result.error("用户名或密码错误");
        }

        // 4. 生成JWT Token（传入用户ID和用户名）
        String token = jwtUtil.generateToken(dbUser.getId(), dbUser.getUsername());

        // 5. 返回结果（包含Token）
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", dbUser.getUsername());
        data.put("userId", dbUser.getId());
        return Result.successWithMessage("登录成功", data);
    }

    /**
     * 获取用户列表接口（/api/user/list）
     */
    @GetMapping("/api/user/list")
    public Result getList() {
        // 查询所有用户
        List<User> users = userMapper.selectList(null);
        return Result.success(users);
    }

    /**
     * 测试接口（无需删除，用于验证认证）
     */
    @PostMapping("/api/test/auth")
    public Result testAuth() {
        return Result.successWithMessage("接口访问成功（已通过JWT认证）");
    }
}