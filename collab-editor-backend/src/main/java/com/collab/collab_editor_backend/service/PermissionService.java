package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.Permission;
import com.collab.collab_editor_backend.util.Result;
import java.util.List;
import java.util.Map;

/**
 * 权限服务接口
 */
public interface PermissionService {
    /**
     * 获取所有权限
     */
    Result<List<Permission>> getAllPermissions();
    
    /**
     * 根据权限ID获取权限
     */
    Result<Permission> getPermissionById(Long id);
    
    /**
     * 创建权限
     */
    Result<Permission> createPermission(Permission permission);
    
    /**
     * 更新权限
     */
    Result<Permission> updatePermission(Long id, Permission permission);
    
    /**
     * 删除权限
     */
    Result<?> deletePermission(Long id);
    
    /**
     * 根据角色名称获取权限列表
     */
    Result<List<Permission>> getPermissionsByRoleName(String roleName);
}
