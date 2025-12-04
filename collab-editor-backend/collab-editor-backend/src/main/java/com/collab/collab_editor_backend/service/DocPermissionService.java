package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.DocPermission;
import java.util.List;

/**
 * 文档权限模块服务层接口
 */
public interface DocPermissionService {
    
    /**
     * 为用户分配文档权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @param permissionType 权限类型（0-查看，1-编辑）
     * @return 创建的权限记录
     */
    DocPermission assignPermission(Long docId, Long userId, Integer permissionType);
    
    /**
     * 移除用户的文档权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 是否移除成功
     */
    boolean removePermission(Long docId, Long userId);
    
    /**
     * 更新用户的文档权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @param permissionType 新的权限类型（0-查看，1-编辑）
     * @return 是否更新成功
     */
    boolean updatePermission(Long docId, Long userId, Integer permissionType);
    
    /**
     * 根据文档ID获取所有权限记录
     * @param docId 文档ID
     * @return 权限记录列表
     */
    List<DocPermission> getPermissionsByDocId(Long docId);
    
    /**
     * 获取用户在指定文档的权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 权限记录，null表示无权限
     */
    DocPermission getPermissionByDocIdAndUserId(Long docId, Long userId);
    
    /**
     * 检查用户是否有指定文档的查看权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 是否有查看权限
     */
    boolean hasViewPermission(Long docId, Long userId);
    
    /**
     * 检查用户是否有指定文档的编辑权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 是否有编辑权限
     */
    boolean hasEditPermission(Long docId, Long userId);
    
    /**
     * 获取用户有权限的所有文档ID列表
     * @param userId 用户ID
     * @param permissionType 权限类型（可选，null表示所有权限）
     * @return 文档ID列表
     */
    List<Long> getDocIdsByUserId(Long userId, Integer permissionType);
}
