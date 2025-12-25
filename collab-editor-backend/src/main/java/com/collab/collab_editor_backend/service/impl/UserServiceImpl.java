package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.collab_editor_backend.dto.UserLoginDTO;
import com.collab.collab_editor_backend.dto.UserProfileDTO;
import com.collab.collab_editor_backend.dto.UserRegisterDTO;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    // 注入用户Mapper（数据库操作）
    @Autowired
    private UserMapper userMapper;

    // 注入JWT工具类（生成令牌）
    @Autowired
    private JwtUtil jwtUtil;

    // 注入操作日志服务
    @Autowired
    private com.collab.collab_editor_backend.service.OperationLogService operationLogService;

    // 密码加密器（Spring Security提供）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册实现
     */
    @Override
    public Result<?> register(UserRegisterDTO dto) {
        // 1. 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            // 记录操作日志
            operationLogService.recordLog(
                null, // 注册时还没有用户ID
                dto.getUsername(), // 用户名
                "register", // 操作类型
                "用户注册失败：用户名已被占用", // 操作内容
                "127.0.0.1", // IP地址（默认值）
                "", // User-Agent（默认值）
                false, // 操作失败
                "用户名已被占用，请更换" // 错误信息
            );
            return Result.error("用户名已被占用，请更换");
        }

        // 2. 构建用户实体（密码加密）
        User user = new User();
        user.setUsername(dto.getUsername());
        // 密码加密存储（避免明文）
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 昵称默认与用户名一致（若DTO传了昵称则用DTO的）
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        // 设置默认角色
        user.setRole("editor");

        // 3. 保存用户到数据库
        userMapper.insert(user);

        // 记录操作日志
        operationLogService.recordLog(
            null, // 注册时还没有用户ID
            dto.getUsername(), // 用户名
            "register", // 操作类型
            "用户注册成功", // 操作内容
            "127.0.0.1", // IP地址（默认值）
            "", // User-Agent（默认值）
            true, // 操作成功
            null // 错误信息
        );

        return Result.successWithMessage("注册成功，请登录");
    }

    /**
     * 用户登录实现
     */
    @Override
    public String login(UserLoginDTO dto) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 验证密码（加密后比对）
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 生成JWT令牌（传递用户ID和用户名）
        return jwtUtil.generateToken(user.getId(), user.getUsername());
    }
    
    /**
     * 根据用户名列表查询用户实现
     */
    @Override
    public List<User> getUsersByUsernameList(List<String> usernameList) {
        if (usernameList == null || usernameList.isEmpty()) {
            return List.of();
        }
        return userMapper.selectByUsernameList(usernameList);
    }
    
    /**
     * 根据用户ID列表查询用户实现
     */
    @Override
    public List<User> getUsersByIdList(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(userIds);
    }
    
    /**
     * 获取用户信息实现
     */
    @Override
    public Result<User> getUserInfo(Long userId) {
        // 根据用户ID查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        // 隐藏敏感信息
        user.setPassword(null);
        return Result.success(user);
    }
    
    /**
     * 更新用户信息实现
     */
    @Override
    public Result<?> updateUserInfo(Long userId, UserProfileDTO dto) {
        // 验证用户是否存在
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            return Result.error("用户不存在");
        }
        
        // 验证邮箱唯一性（如果用户修改了邮箱）
        if (dto.getEmail() != null && !dto.getEmail().equals(existingUser.getEmail())) {
            User userByEmail = userMapper.selectByEmail(dto.getEmail());
            if (userByEmail != null && !userByEmail.getId().equals(userId)) {
                return Result.error("该邮箱已被其他用户使用");
            }
        }
        
        // 验证手机号唯一性（如果用户修改了手机号）
        if (dto.getPhone() != null && !dto.getPhone().equals(existingUser.getPhone())) {
            User userByPhone = userMapper.selectByPhone(dto.getPhone());
            if (userByPhone != null && !userByPhone.getId().equals(userId)) {
                return Result.error("该手机号已被其他用户使用");
            }
        }
        
        // 更新用户信息（只更新允许修改的字段）
        if (dto.getNickname() != null) {
            existingUser.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            existingUser.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            existingUser.setPhone(dto.getPhone());
        }
        if (dto.getAvatarUrl() != null) {
            existingUser.setAvatarUrl(dto.getAvatarUrl());
        }
        existingUser.setUpdateTime(java.time.LocalDateTime.now());
        
        // 保存更新
        userMapper.updateById(existingUser);
        
        return Result.successWithMessage("用户信息更新成功");
    }
    
    /**
     * 修改密码实现
     */
    @Override
    public Result<?> changePassword(Long userId, String oldPassword, String newPassword) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 验证旧密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("旧密码错误");
        }
        
        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(java.time.LocalDateTime.now());
        
        // 保存更新
        userMapper.updateById(user);
        
        return Result.successWithMessage("密码修改成功");
    }
    
    /**
     * 请求密码重置实现
     */
    @Override
    public Result<?> requestPasswordReset(String identifier) {
        User user = null;
        String message = "";
        
        // 判断输入是邮箱还是手机号
        if (identifier.contains("@")) {
            // 邮箱格式
            user = userMapper.selectByEmail(identifier);
            if (user == null) {
                return Result.error("该邮箱未注册");
            }
            message = "密码重置链接已发送到您的邮箱";
        } else {
            // 手机号格式
            user = userMapper.selectByPhone(identifier);
            if (user == null) {
                return Result.error("该手机号未注册");
            }
            message = "密码重置验证码已发送到您的手机";
        }
        
        // 生成随机重置令牌
        String resetToken = UUID.randomUUID().toString().replaceAll("-", "");
        
        // 设置令牌过期时间（24小时后）
        LocalDateTime expiryTime = LocalDateTime.now().plusHours(24);
        
        // 更新用户的重置令牌和过期时间
        userMapper.updateResetToken(user.getId(), resetToken, expiryTime);
        
        // TODO: 根据用户联系方式发送重置信息
        // 1. 如果是邮箱，发送重置邮件（集成邮件服务）
        // 2. 如果是手机号，发送短信验证码（集成短信服务）
        
        // 返回令牌信息，方便测试
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("resetToken", resetToken);
        return new Result<>(200, message, data);
    }
    
    /**
     * 使用重置令牌重置密码实现
     */
    @Override
    public Result<?> resetPassword(String resetToken, String newPassword) {
        // 根据重置令牌查询有效的用户
        User user = userMapper.selectByResetToken(resetToken);
        if (user == null) {
            return Result.error("无效的重置令牌或令牌已过期");
        }
        
        // 加密新密码
        String encryptedPassword = passwordEncoder.encode(newPassword);
        
        // 更新密码
        user.setPassword(encryptedPassword);
        user.setUpdateTime(LocalDateTime.now());
        
        // 保存密码更新
        userMapper.updateById(user);
        
        // 清除重置令牌
        userMapper.clearResetToken(user.getId());
        
        return Result.successWithMessage("密码重置成功");
    }

    /**
     * 获取用户列表实现（分页、筛选、排序）
     */
    @Override
    public Result<Map<String, Object>> getUserList(int page, int pageSize, Map<String, Object> filters, String sortBy, String sortOrder) {
        // 创建分页对象
        Page<User> userPage = new Page<>(page, pageSize);
        
        // 创建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        // 筛选条件
        if (filters != null) {
            // 按用户名筛选
            if (filters.containsKey("username")) {
                queryWrapper.like("username", filters.get("username"));
            }
            // 按角色筛选
            if (filters.containsKey("role")) {
                queryWrapper.eq("role", filters.get("role"));
            }
            // 按邮箱筛选
            if (filters.containsKey("email")) {
                queryWrapper.like("email", filters.get("email"));
            }
        }
        
        // 排序条件
        if (sortBy != null && !sortBy.isEmpty()) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                queryWrapper.orderByAsc(getColumnFromFieldName(sortBy));
            } else {
                queryWrapper.orderByDesc(getColumnFromFieldName(sortBy));
            }
        } else {
            // 默认按创建时间降序
            queryWrapper.orderByDesc("create_time");
        }
        
        // 执行分页查询
        IPage<User> resultPage = userMapper.selectPage(userPage, queryWrapper);
        
        // 隐藏敏感信息
        List<User> users = resultPage.getRecords();
        users.forEach(user -> user.setPassword(null));
        
        // 构建返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("total", resultPage.getTotal());
        response.put("pages", resultPage.getPages());
        response.put("current", resultPage.getCurrent());
        response.put("pageSize", resultPage.getSize());
        
        return Result.success(response);
    }
    
    /**
     * 将Java字段名转换为数据库列名
     */
    private String getColumnFromFieldName(String fieldName) {
        // 简单实现：驼峰转下划线
        StringBuilder columnName = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                columnName.append("_");
                columnName.append(Character.toLowerCase(c));
            } else {
                columnName.append(c);
            }
        }
        return columnName.toString();
    }
    
    /**
     * 修改用户角色实现
     */
    @Override
    public Result<?> updateUserRole(Long userId, String newRole) {
        // 验证角色是否有效
        if (newRole == null || !isValidRole(newRole)) {
            return Result.error("无效的角色类型");
        }
        
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        String oldRole = user.getRole();
        // 更新角色
        user.setRole(newRole);
        user.setUpdateTime(LocalDateTime.now());
        
        // 保存更新
        userMapper.updateById(user);
        
        // 记录操作日志
        operationLogService.recordLog(
            userId, // 操作用户ID（这里应该是当前登录用户ID，暂时使用被操作用户ID）
            user.getUsername(), // 操作用户名（这里应该是当前登录用户名，暂时使用被操作用户名）
            "update_role", // 操作类型
            "用户角色更新：用户ID " + userId + "，原角色 " + oldRole + "，新角色 " + newRole, // 操作内容
            "127.0.0.1", // IP地址（这里应该从请求中获取，暂时使用默认值）
            "", // User-Agent（这里应该从请求中获取，暂时使用空值）
            true, // 操作成功
            null // 错误信息
        );
        
        return Result.successWithMessage("用户角色更新成功");
    }
    
    /**
     * 验证角色是否有效
     */
    private boolean isValidRole(String role) {
        return "admin".equals(role) || "editor".equals(role) || "viewer".equals(role);
    }
}