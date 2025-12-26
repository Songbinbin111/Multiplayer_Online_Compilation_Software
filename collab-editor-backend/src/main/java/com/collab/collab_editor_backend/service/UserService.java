package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.dto.UserLoginDTO;
import com.collab.collab_editor_backend.dto.UserProfileDTO;
import com.collab.collab_editor_backend.dto.UserRegisterDTO;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.util.Result;

import java.util.List;
import java.util.Map;

public interface UserService {
    /**
     * 获取用户列表（分页、筛选、排序）
     * @param page 页码
     * @param pageSize 每页条数
     * @param filters 筛选条件
     * @param sortBy 排序字段
     * @param sortOrder 排序顺序（asc/desc）
     * @return 分页用户列表
     */
    Result<Map<String, Object>> getUserList(int page, int pageSize, Map<String, Object> filters, String sortBy, String sortOrder);
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
    
    /**
     * 根据用户ID列表查询用户
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    List<User> getUsersByIdList(List<Long> userIds);
    
    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    Result<User> getUserInfo(Long userId);
    
    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param dto 用户资料DTO
     * @return 更新结果
     */
    Result<?> updateUserInfo(Long userId, UserProfileDTO dto);
    
    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    Result<?> changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 请求密码重置
     * @param identifier 用户标识（邮箱或手机号）
     * @return 重置结果
     */
    Result<?> requestPasswordReset(String identifier);
    
    /**
     * 使用重置令牌重置密码
     * @param resetToken 重置令牌
     * @param newPassword 新密码
     * @return 重置结果
     */
    Result<?> resetPassword(String resetToken, String newPassword);
    
    /**
     * 修改用户角色
     * @param userId 用户ID
     * @param newRole 新角色
     * @return 修改结果
     */
    Result<?> updateUserRole(Long userId, String newRole);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 删除结果
     */
    Result<?> deleteUser(Long userId);
}