package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.dto.UserLoginDTO;
import com.collab.collab_editor_backend.dto.UserRegisterDTO;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.util.Result;

import java.util.List;

public interface UserService {
    /**
     * 用户注册
     * @param dto 注册参数（用户名、密码、昵称）
     */
    Result<?> register(UserRegisterDTO dto);

    /**
     * 用户登录
     * @param dto 登录参数（用户名、密码）
     * @return JWT令牌
     */
    String login(UserLoginDTO dto);
    
    /**
     * 根据用户名列表查询用户
     * @param usernameList 用户名列表
     * @return 用户列表
     */
    List<User> getUsersByUsernameList(List<String> usernameList);
}