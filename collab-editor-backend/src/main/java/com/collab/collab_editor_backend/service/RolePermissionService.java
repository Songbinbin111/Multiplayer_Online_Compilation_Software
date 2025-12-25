package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.util.Result;
import java.util.List;

/**
 * 角色权限服务接口
 */
public interface RolePermissionService {
    /**
     * 为角色分配权限
     */
    Result<?> assignPermissionsToRole(String roleName, List<Long> permissionIds);
    
    /**
     * 获取角色的权限ID列表
     */
    Result<List<Long>> getPermissionIdsByRoleName(String roleName);
    
    /**
     * 获取角色的权限列表
     */
    Result<?> getPermissionsByRoleName(String roleName);
    
    /**
     * 移除角色的指定权限
     */
    Result<?> removePermissionFromRole(String roleName, Long permissionId);
    
    /**
     * 清除角色的所有权限
     */
    Result<?> clearRolePermissions(String roleName);
}
