package com.collab.collab_editor_backend.util;

import com.collab.collab_editor_backend.entity.Permission;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.mapper.PermissionMapper;
import com.collab.collab_editor_backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 权限检查工具类
 */
@Component
public class PermissionUtil {
    
    @Autowired
    private PermissionMapper permissionMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 检查用户是否拥有某个权限
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否拥有权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        // 获取用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 管理员拥有所有权限
        if ("admin".equals(user.getRole())) {
            return true;
        }
        
        // 获取用户角色的所有权限
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleName(user.getRole());
        
        // 检查是否拥有指定权限
        for (Permission permission : permissions) {
            if (permissionCode.equals(permission.getPermissionCode())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查用户是否拥有多个权限中的至少一个
     * @param userId 用户ID
     * @param permissionCodes 权限编码列表
     * @return 是否拥有至少一个权限
     */
    public boolean hasAnyPermission(Long userId, List<String> permissionCodes) {
        // 获取用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 管理员拥有所有权限
        if ("admin".equals(user.getRole())) {
            return true;
        }
        
        // 获取用户角色的所有权限
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleName(user.getRole());
        
        // 检查是否拥有任意一个权限
        for (Permission permission : permissions) {
            if (permissionCodes.contains(permission.getPermissionCode())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查用户是否拥有所有指定权限
     * @param userId 用户ID
     * @param permissionCodes 权限编码列表
     * @return 是否拥有所有权限
     */
    public boolean hasAllPermissions(Long userId, List<String> permissionCodes) {
        // 获取用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        // 管理员拥有所有权限
        if ("admin".equals(user.getRole())) {
            return true;
        }
        
        // 获取用户角色的所有权限
        List<Permission> permissions = permissionMapper.selectPermissionsByRoleName(user.getRole());
        
        // 提取权限编码
        List<String> userPermissionCodes = permissions.stream()
                .map(Permission::getPermissionCode)
                .toList();
        
        // 检查是否包含所有指定权限
        return userPermissionCodes.containsAll(permissionCodes);
    }
}
