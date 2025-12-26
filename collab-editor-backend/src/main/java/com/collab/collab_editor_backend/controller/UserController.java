package com.collab.collab_editor_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;
import com.collab.collab_editor_backend.dto.UserProfileDTO;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.service.OperationLogService;
import com.collab.collab_editor_backend.service.UserService;
import com.collab.collab_editor_backend.service.UserActivityService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.MinIOUtil;
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

    @Autowired
    private UserActivityService userActivityService;

    // 注入用户服务
    @Autowired
    private UserService userService;

    @Autowired
    private MinIOUtil minIOUtil;

    // 注入操作日志服务
    @Autowired
    private OperationLogService operationLogService;

    /**
     * 注册接口（/api/register）- 支持邮箱/手机号注册
     */
    @PostMapping("/api/register")
    public Result register(@RequestBody User user) {
        logger.info("收到注册请求，用户名: {}, 邮箱: {}, 手机号: {}", user.getUsername(), user.getEmail(), user.getPhone());
        
        // 1. 校验参数
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            logger.error("注册失败：密码不能为空");
            return Result.error("密码不能为空");
        }
        
        logger.info("密码校验通过");
        
        // 2. 至少需要提供用户名、邮箱或手机号中的一个作为唯一标识
        if ((user.getUsername() == null || user.getUsername().trim().isEmpty()) &&
            (user.getEmail() == null || user.getEmail().trim().isEmpty()) &&
            (user.getPhone() == null || user.getPhone().trim().isEmpty())) {
            logger.error("注册失败：至少需要提供用户名、邮箱或手机号中的一个");
            return Result.error("至少需要提供用户名、邮箱或手机号中的一个");
        }
        
        logger.info("唯一标识校验通过");

        // 3. 自动生成用户名（如果未提供）
        String username = user.getUsername();
        String email = user.getEmail();
        String phone = user.getPhone();
        
        logger.info("生成用户名前 - 用户名: {}, 邮箱: {}, 手机号: {}", username, email, phone);
        
        // 直接检查前端传入的参数
        if (username == null || username.trim().isEmpty()) {
            logger.info("用户名为空，需要自动生成");
            if (email != null && !email.trim().isEmpty()) {
                // 使用邮箱前缀作为用户名
                username = email.split("@")[0];
                logger.info("从邮箱生成用户名: {}", username);
            } else if (phone != null && !phone.trim().isEmpty()) {
                // 使用手机号作为用户名
                username = phone;
                logger.info("从手机号生成用户名: {}", username);
            } else {
                logger.error("注册失败：无法生成用户名");
                return Result.error("无法生成用户名，请联系管理员");
            }
            user.setUsername(username);
            logger.info("设置用户名后: {}", user.getUsername());
        } else {
            username = username.trim();
            logger.info("使用用户提供的用户名: {}", username);
        }

        // 4. 校验用户名是否已存在
        logger.info("开始校验用户名是否已存在: {}", username);
        User existingUser = userMapper.selectByUsername(username);
        if (existingUser != null) {
            logger.error("注册失败：用户名 {} 已被占用", username);
            return Result.error("用户名已被占用，请更换");
        }
        
        logger.info("用户名校验通过");
        
        // 5. 校验邮箱是否已存在
        if (email != null && !email.isEmpty()) {
            existingUser = userMapper.selectByEmail(email);
            if (existingUser != null) {
                logger.error("注册失败：邮箱 {} 已被占用", email);
                return Result.error("邮箱已被占用，请更换");
            }
        }
        
        // 6. 校验手机号是否已存在
        if (phone != null && !phone.isEmpty()) {
            existingUser = userMapper.selectByPhone(phone);
            if (existingUser != null) {
                logger.error("注册失败：手机号 {} 已被占用", phone);
                return Result.error("手机号已被占用，请更换");
            }
        }

        // 6. 密码加密（BCrypt算法，不可逆）
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encryptedPassword);
        
        // 7. 设置默认角色
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("editor"); // 默认角色为编辑者
        }
        
        // 8. 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        // 9. 保存用户到数据库
        int insert = userMapper.insert(user);
        if (insert > 0) {
            logger.info("注册成功：用户名 {}", user.getUsername());
            // 记录注册行为
            userActivityService.recordActivity(user.getId(), "register", "用户注册成功");
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
    public Result login(@RequestBody User user, HttpServletRequest request) {
        // 1. 校验参数
        if (user.getUsername() == null || user.getPassword() == null) {
            // 记录登录失败日志
            operationLogService.recordLog(
                null, // 登录失败时还没有用户ID
                user.getUsername(), // 用户名
                "login", // 操作类型
                "登录失败：用户名或密码不能为空", // 操作内容
                request.getRemoteAddr(), // IP地址
                request.getHeader("User-Agent"), // User-Agent
                false, // 操作失败
                "用户名或密码不能为空" // 错误信息
            );
            return Result.error("用户名或密码不能为空");
        }

        // 2. 查询数据库中的用户
        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser == null) {
            // 记录登录失败日志
            operationLogService.recordLog(
                null, // 登录失败时还没有用户ID
                user.getUsername(), // 用户名
                "login", // 操作类型
                "登录失败：用户名不存在", // 操作内容
                request.getRemoteAddr(), // IP地址
                request.getHeader("User-Agent"), // User-Agent
                false, // 操作失败
                "用户名或密码错误" // 错误信息
            );
            return Result.error("用户名或密码错误");
        }

        // 3. 比对密码（加密后的密码无法解密，用matches方法校验）
        boolean passwordMatch = passwordEncoder.matches(user.getPassword(), dbUser.getPassword());
        if (!passwordMatch) {
            // 记录登录失败日志
            operationLogService.recordLog(
                dbUser.getId(), // 用户ID
                dbUser.getUsername(), // 用户名
                "login", // 操作类型
                "登录失败：密码错误", // 操作内容
                request.getRemoteAddr(), // IP地址
                request.getHeader("User-Agent"), // User-Agent
                false, // 操作失败
                "用户名或密码错误" // 错误信息
            );
            return Result.error("用户名或密码错误");
        }

        // 4. 生成JWT Token（传入用户ID和用户名）
        String token = jwtUtil.generateToken(dbUser.getId(), dbUser.getUsername());

        // 5. 记录登录行为
        userActivityService.recordActivity(dbUser.getId(), "login", "用户登录成功");
        
        // 6. 记录登录成功日志
        operationLogService.recordLog(
            dbUser.getId(), // 用户ID
            dbUser.getUsername(), // 用户名
            "login", // 操作类型
            "登录成功", // 操作内容
            request.getRemoteAddr(), // IP地址
            request.getHeader("User-Agent"), // User-Agent
            true, // 操作成功
            null // 无错误信息
        );
        
        // 7. 返回结果（包含Token）
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", dbUser.getUsername());
        data.put("userId", dbUser.getId());
        data.put("role", dbUser.getRole());
        return Result.successWithMessage("登录成功", data);
    }

    /**
     * 获取用户列表接口（/api/user/list）- 旧版，兼容使用
     */
    @GetMapping("/api/user/list")
    public Result getList(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return Result.error("未认证");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            return Result.error("权限不足");
        }
        List<User> users = userMapper.selectList(null);
        users.forEach(user -> user.setPassword(null));
        return Result.success(users);
    }
    
    /**
     * 获取用户列表接口（分页、筛选、排序）
     */
    @GetMapping("/api/user/list/page")
    public Result getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId == null) {
            return Result.error("未认证");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            return Result.error("权限不足");
        }
        // 构建筛选条件
        Map<String, Object> filters = new HashMap<>();
        if (username != null && !username.isEmpty()) {
            filters.put("username", username);
        }
        if (role != null && !role.isEmpty()) {
            filters.put("role", role);
        }
        if (email != null && !email.isEmpty()) {
            filters.put("email", email);
        }
        
        return userService.getUserList(page, pageSize, filters, sortBy, sortOrder);
    }

    /**
     * 更新用户信息接口
     */
    @PostMapping("/api/user/update")
    public Result updateUser(@RequestBody UserProfileDTO dto) {
        logger.info("收到更新用户信息请求，用户ID: {}", dto.getId());
        
        // 1. 校验参数
        if (dto.getId() == null) {
            logger.error("更新失败：用户ID不能为空");
            return Result.error("用户ID不能为空");
        }
        
        // 2. 调用服务层更新用户信息
        return userService.updateUserInfo(dto.getId(), dto);
    }
    
    /**
     * 上传头像接口
     */
    @PostMapping("/api/user/avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        logger.info("收到上传头像请求");
        
        try {
            // 使用MinIOUtil上传头像文件
            String avatarUrl = minIOUtil.uploadFile(file);
            
            return Result.successWithMessage("上传成功", avatarUrl);
        } catch (Exception e) {
            logger.error("上传头像失败: {}", e.getMessage(), e);
            return Result.error("上传头像失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取个人资料接口
     */
    @GetMapping("/api/user/profile")
    public Result getUserProfile(@RequestParam("userId") Long userId) {
        logger.info("收到获取用户资料请求，用户ID: {}", userId);
        
        return userService.getUserInfo(userId);
    }

    /**
     * 修改密码接口
     */
    @PostMapping("/api/user/changePassword")
    public Result changePassword(@RequestParam("userId") Long userId, 
                                @RequestParam("oldPassword") String oldPassword, 
                                @RequestParam("newPassword") String newPassword) {
        logger.info("收到修改密码请求，用户ID: {}", userId);
        
        return userService.changePassword(userId, oldPassword, newPassword);
    }
    
    /**
     * 请求密码重置接口
     */
    @PostMapping("/api/user/reset-password/request")
    public Result requestPasswordReset(@RequestBody com.collab.collab_editor_backend.dto.PasswordResetRequestDTO dto) {
        logger.info("收到密码重置请求，标识: {}", dto.getIdentifier());
        return userService.requestPasswordReset(dto.getIdentifier());
    }
    
    /**
     * 使用重置令牌重置密码接口
     */
    @PostMapping("/api/user/reset-password")
    public Result resetPassword(@RequestBody com.collab.collab_editor_backend.dto.PasswordResetDTO dto) {
        logger.info("收到使用令牌重置密码请求");
        return userService.resetPassword(dto.getToken(), dto.getNewPassword());
    }
    
    /**
     * 刷新JWT令牌接口
     */
    @PostMapping("/api/refresh-token")
    public Result refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("收到刷新令牌请求");
        
        try {
            // 解析当前令牌（支持过期令牌）
            Long userId = jwtUtil.getUserIdFromExpiredToken(authorizationHeader);
            
            // 检查用户是否存在
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 生成新令牌
            String newToken = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            // 返回新令牌
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            return Result.successWithMessage("令牌刷新成功", data);
        } catch (Exception e) {
            logger.error("刷新令牌失败: {}", e.getMessage());
            return Result.error("刷新令牌失败: " + e.getMessage());
        }
    }
    
    /**
     * 修改用户角色接口
     */
    @PostMapping("/api/user/update-role")
    public Result updateUserRole(
            @RequestParam Long userId,
            @RequestParam String newRole) {
        logger.info("收到修改用户角色请求，用户ID: {}, 新角色: {}", userId, newRole);
        
        return userService.updateUserRole(userId, newRole);
    }

    /**
     * 删除用户接口
     */
    @PostMapping("/api/user/delete")
    public Result deleteUser(@RequestParam Long userId) {
        logger.info("收到删除用户请求，用户ID: {}", userId);
        return userService.deleteUser(userId);
    }
}
