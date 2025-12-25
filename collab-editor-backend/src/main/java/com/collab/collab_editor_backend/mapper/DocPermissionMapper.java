package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.DocPermission;
import java.util.List;

/**
 * 文档权限模块Mapper接口
 */
public interface DocPermissionMapper extends BaseMapper<DocPermission> {
    
    /**
     * 根据文档ID获取所有权限记录
     * @param docId 文档ID
     * @return 权限记录列表
     */
    List<DocPermission> getPermissionsByDocId(Long docId);
    
    /**
     * 根据用户ID获取所有有权限的文档ID列表
     * @param userId 用户ID
     * @param permissionType 权限类型（可选，null表示所有权限）
     * @return 文档ID列表
     */
    List<Long> getDocIdsByUserId(Long userId, Integer permissionType);
    
    /**
     * 获取用户在指定文档的权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 权限记录，null表示无权限
     */
    DocPermission getPermissionByDocIdAndUserId(Long docId, Long userId);
}
