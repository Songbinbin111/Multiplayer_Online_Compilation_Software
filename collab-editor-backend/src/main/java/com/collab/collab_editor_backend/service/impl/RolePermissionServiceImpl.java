package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.collab.collab_editor_backend.entity.RolePermission;
import com.collab.collab_editor_backend.mapper.RolePermissionMapper;
import com.collab.collab_editor_backend.service.PermissionService;
import com.collab.collab_editor_backend.service.RolePermissionService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色权限服务实现类
 */
@Service
public class RolePermissionServiceImpl implements RolePermissionService {
    
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    
    @Autowired
    private PermissionService permissionService;
    
    @Override
    public Result<?> assignPermissionsToRole(String roleName, List<Long> permissionIds) {
        // 验证角色是否有效
        if (!isValidRole(roleName)) {
            return Result.error("无效的角色名称");
        }
        
        // 清除角色原有权限
        QueryWrapper<RolePermission> clearWrapper = new QueryWrapper<>();
        clearWrapper.eq("role_name", roleName);
        rolePermissionMapper.delete(clearWrapper);
        
        // 为角色分配新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleName(roleName);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
        
        return Result.successWithMessage("权限分配成功");
    }
    
    @Override
    public Result<List<Long>> getPermissionIdsByRoleName(String roleName) {
        // 使用QueryWrapper替代自定义方法
        QueryWrapper<RolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_name", roleName);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(queryWrapper);
        
        // 提取权限ID列表
        List<Long> permissionIds = new java.util.ArrayList<>();
        for (RolePermission rp : rolePermissions) {
            permissionIds.add(rp.getPermissionId());
        }
        
        return Result.success(permissionIds);
    }
    
    @Override
    public Result<?> getPermissionsByRoleName(String roleName) {
        // 直接调用PermissionService获取权限列表
        return permissionService.getPermissionsByRoleName(roleName);
    }
    
    @Override
    public Result<?> removePermissionFromRole(String roleName, Long permissionId) {
        // 验证角色是否有效
        if (!isValidRole(roleName)) {
            return Result.error("无效的角色名称");
        }
        
        // 删除角色权限关联
        QueryWrapper<RolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_name", roleName);
        queryWrapper.eq("permission_id", permissionId);
        rolePermissionMapper.delete(queryWrapper);
        
        return Result.successWithMessage("权限移除成功");
    }
    
    @Override
    public Result<?> clearRolePermissions(String roleName) {
        // 验证角色是否有效
        if (!isValidRole(roleName)) {
            return Result.error("无效的角色名称");
        }
        
        // 清除角色所有权限
        QueryWrapper<RolePermission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_name", roleName);
        rolePermissionMapper.delete(queryWrapper);
        
        return Result.successWithMessage("角色权限已清空");
    }
    
    /**
     * 验证角色是否有效
     */
    private boolean isValidRole(String roleName) {
        return "admin".equals(roleName) || "editor".equals(roleName) || "viewer".equals(roleName);
    }
}
