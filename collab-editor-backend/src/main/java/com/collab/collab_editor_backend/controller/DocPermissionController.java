package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.entity.DocPermission;
import com.collab.collab_editor_backend.service.DocPermissionService;
import com.collab.collab_editor_backend.util.JwtUtil;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 文档权限模块Controller
 */
@RestController
@RequestMapping("/api/permission")
public class DocPermissionController {

    @Autowired
    private DocPermissionService docPermissionService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 为用户分配文档权限接口
     * @param docId 文档ID
     * @param userId 用户ID
     * @param permissionType 权限类型
     * @param request 请求对象
     * @return 创建的权限记录
     */
    @PostMapping("/assign/{docId}/{userId}/{permissionType}")
    public Result<?> assignPermission(@PathVariable Long docId, @PathVariable Long userId, 
                                     @PathVariable Integer permissionType, HttpServletRequest request) {
        try {
            // 验证用户身份（只有文档所有者或管理员可以分配权限）
            String authorization = request.getHeader("Authorization");
            Long currentUserId = jwtUtil.getUserIdFromToken(authorization);
            
            // 分配权限
            DocPermission permission = docPermissionService.assignPermission(docId, userId, permissionType);
            return Result.success(permission);
        } catch (Exception e) {
            return Result.error("分配权限失败：" + e.getMessage());
        }
    }

    /**
     * 移除用户的文档权限接口
     * @param docId 文档ID
     * @param userId 用户ID
     * @param request 请求对象
     * @return 移除结果
     */
    @DeleteMapping("/remove/{docId}/{userId}")
    public Result<?> removePermission(@PathVariable Long docId, @PathVariable Long userId, HttpServletRequest request) {
        try {
            // 验证用户身份（只有文档所有者或管理员可以移除权限）
            String authorization = request.getHeader("Authorization");
            Long currentUserId = jwtUtil.getUserIdFromToken(authorization);
            
            boolean success = docPermissionService.removePermission(docId, userId);
            if (success) {
                return Result.success("权限移除成功");
            } else {
                return Result.error("权限移除失败");
            }
        } catch (Exception e) {
            return Result.error("移除权限失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户的文档权限接口
     * @param docId 文档ID
     * @param userId 用户ID
     * @param permissionType 新的权限类型
     * @param request 请求对象
     * @return 更新结果
     */
    @PutMapping("/update/{docId}/{userId}/{permissionType}")
    public Result<?> updatePermission(@PathVariable Long docId, @PathVariable Long userId, 
                                     @PathVariable Integer permissionType, HttpServletRequest request) {
        try {
            // 验证用户身份（只有文档所有者或管理员可以更新权限）
            String authorization = request.getHeader("Authorization");
            Long currentUserId = jwtUtil.getUserIdFromToken(authorization);
            
            boolean success = docPermissionService.updatePermission(docId, userId, permissionType);
            if (success) {
                return Result.success("权限更新成功");
            } else {
                return Result.error("权限更新失败");
            }
        } catch (Exception e) {
            return Result.error("更新权限失败：" + e.getMessage());
        }
    }

    /**
     * 根据文档ID获取所有权限记录接口
     * @param docId 文档ID
     * @param request 请求对象
     * @return 权限记录列表
     */
    @GetMapping("/list/{docId}")
    public Result<?> getPermissionsByDocId(@PathVariable Long docId, HttpServletRequest request) {
        try {
            // 验证用户身份（只有文档所有者或有访问权限的用户可以查看权限列表）
            String authorization = request.getHeader("Authorization");
            Long currentUserId = jwtUtil.getUserIdFromToken(authorization);
            
            List<DocPermission> permissions = docPermissionService.getPermissionsByDocId(docId);
            return Result.success(permissions);
        } catch (Exception e) {
            return Result.error("获取权限列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户在指定文档的权限接口
     * @param docId 文档ID
     * @param request 请求对象
     * @return 权限记录
     */
    @GetMapping("/check/{docId}")
    public Result<?> getPermissionByDocId(@PathVariable Long docId, HttpServletRequest request) {
        try {
            // 获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long currentUserId = jwtUtil.getUserIdFromToken(authorization);
            
            DocPermission permission = docPermissionService.getPermissionByDocIdAndUserId(docId, currentUserId);
            return Result.success(permission);
        } catch (Exception e) {
            return Result.error("获取权限失败：" + e.getMessage());
        }
    }

    /**
     * 检查用户是否有文档的查看权限接口
     * @param docId 文档ID
     * @param request 请求对象
     * @return 检查结果
     */
    @GetMapping("/check/view/{docId}")
    public Result<?> checkViewPermission(@PathVariable Long docId, HttpServletRequest request) {
        try {
            // 获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long currentUserId = jwtUtil.getUserIdFromToken(authorization);
            
            boolean hasPermission = docPermissionService.hasViewPermission(docId, currentUserId);
            return Result.success(hasPermission);
        } catch (Exception e) {
            return Result.error("检查权限失败：" + e.getMessage());
        }
    }

    /**
     * 检查用户是否有文档的编辑权限接口
     * @param docId 文档ID
     * @param request 请求对象
     * @return 检查结果
     */
    @GetMapping("/check/edit/{docId}")
    public Result<?> checkEditPermission(@PathVariable Long docId, HttpServletRequest request) {
        try {
            // 获取当前用户ID
            String authorization = request.getHeader("Authorization");
            Long currentUserId = jwtUtil.getUserIdFromToken(authorization);
            
            boolean hasPermission = docPermissionService.hasEditPermission(docId, currentUserId);
            return Result.success(hasPermission);
        } catch (Exception e) {
            return Result.error("检查权限失败：" + e.getMessage());
        }
    }
}
