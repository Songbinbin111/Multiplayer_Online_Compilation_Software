package com.collab.collab_editor_backend.service.impl;

import com.collab.collab_editor_backend.dto.UserLoginDTO;
import com.collab.collab_editor_backend.dto.UserRegisterDTO;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.service.UserService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    // 注入用户Mapper（数据库操作）
    @Autowired
    private UserMapper userMapper;

    // 注入JWT工具类（生成令牌）
    @Autowired
    private JwtUtil jwtUtil;

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
            return Result.error("用户名已被占用，请更换");
        }

        // 2. 构建用户实体（密码加密）
        User user = new User();
        user.setUsername(dto.getUsername());
        // 密码加密存储（避免明文）
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 昵称默认与用户名一致（若DTO传了昵称则用DTO的）
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());

        // 3. 保存用户到数据库
        userMapper.insert(user);

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
}