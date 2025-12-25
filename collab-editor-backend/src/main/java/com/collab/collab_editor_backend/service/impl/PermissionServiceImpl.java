package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.collab.collab_editor_backend.entity.Permission;
import com.collab.collab_editor_backend.mapper.PermissionMapper;
import com.collab.collab_editor_backend.service.PermissionService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 权限服务实现类
 */
@Service
public class PermissionServiceImpl implements PermissionService {
    
    @Autowired
    private PermissionMapper permissionMapper;
    
    @Override
    public Result<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionMapper.selectList(null);
        return Result.success(permissions);
    }
    
    @Override
    public Result<Permission> getPermissionById(Long id) {
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            return Result.error("权限不存在");
        }
        return Result.success(permission);
    }
    
    @Override
    public Result<Permission> createPermission(Permission permission) {
        // 检查权限编码是否已存在
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_code", permission.getPermissionCode());
        if (permissionMapper.selectOne(queryWrapper) != null) {
            return Result.error("权限编码已存在");
        }
        
        permissionMapper.insert(permission);
        return Result.success(permission);
    }
    
    @Override
    public Result<Permission> updatePermission(Long id, Permission permission) {
        // 检查权限是否存在
        Permission existingPermission = permissionMapper.selectById(id);
        if (existingPermission == null) {
            return Result.error("权限不存在");
        }
        
        // 检查权限编码是否已被其他权限使用
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_code", permission.getPermissionCode());
        queryWrapper.ne("id", id);
        if (permissionMapper.selectOne(queryWrapper) != null) {
            return Result.error("权限编码已存在");
        }
        
        permission.setId(id);
        permissionMapper.updateById(permission);
        return Result.success(permission);
    }
    
    @Override
    public Result<?> deletePermission(Long id) {
        // 检查权限是否存在
        Permission existingPermission = permissionMapper.selectById(id);
        if (existingPermission == null) {
            return Result.error("权限不存在");
        }
        
        permissionMapper.deleteById(id);
        return Result.successWithMessage("权限删除成功");
    }
    
    @Override
    public Result<List<Permission>> getPermissionsByRoleName(String roleName) {
        // 直接返回空列表，因为我们还没有实现角色权限关联的查询
        return Result.success(java.util.Collections.emptyList());
    }
}
