package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.entity.Permission;
import com.collab.collab_editor_backend.service.PermissionService;
import com.collab.collab_editor_backend.service.RolePermissionService;
import com.collab.collab_editor_backend.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器
 */
@RestController
public class PermissionController {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionController.class);
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private RolePermissionService rolePermissionService;
    
    // 权限管理API
    @GetMapping("/api/permissions")
    public Result<List<Permission>> getAllPermissions() {
        logger.info("收到获取所有权限请求");
        return permissionService.getAllPermissions();
    }
    
    @GetMapping("/api/permissions/{id}")
    public Result<Permission> getPermissionById(@PathVariable Long id) {
        logger.info("收到获取权限详情请求，权限ID: {}", id);
        return permissionService.getPermissionById(id);
    }
    
    @PostMapping("/api/permissions")
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        logger.info("收到创建权限请求，权限编码: {}", permission.getPermissionCode());
        return permissionService.createPermission(permission);
    }
    
    @PutMapping("/api/permissions/{id}")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        logger.info("收到更新权限请求，权限ID: {}", id);
        return permissionService.updatePermission(id, permission);
    }
    
    @DeleteMapping("/api/permissions/{id}")
    public Result<?> deletePermission(@PathVariable Long id) {
        logger.info("收到删除权限请求，权限ID: {}", id);
        return permissionService.deletePermission(id);
    }
    
    // 角色权限管理API
    @PostMapping("/api/roles/{roleName}/permissions")
    public Result<?> assignPermissionsToRole(@PathVariable String roleName, @RequestBody List<Long> permissionIds) {
        logger.info("收到为角色分配权限请求，角色: {}", roleName);
        return rolePermissionService.assignPermissionsToRole(roleName, permissionIds);
    }
    
    @GetMapping("/api/roles/{roleName}/permissions")
    public Result<?> getPermissionsByRoleName(@PathVariable String roleName) {
        logger.info("收到获取角色权限请求，角色: {}", roleName);
        return rolePermissionService.getPermissionsByRoleName(roleName);
    }
    
    @DeleteMapping("/api/roles/{roleName}/permissions/{permissionId}")
    public Result<?> removePermissionFromRole(@PathVariable String roleName, @PathVariable Long permissionId) {
        logger.info("收到移除角色权限请求，角色: {}，权限ID: {}", roleName, permissionId);
        return rolePermissionService.removePermissionFromRole(roleName, permissionId);
    }
    
    @DeleteMapping("/api/roles/{roleName}/permissions")
    public Result<?> clearRolePermissions(@PathVariable String roleName) {
        logger.info("收到清除角色所有权限请求，角色: {}", roleName);
        return rolePermissionService.clearRolePermissions(roleName);
    }
}
