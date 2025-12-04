package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.DocumentVersion;
import com.collab.collab_editor_backend.util.Result;

import java.util.List;

/**
 * 文档版本服务接口
 */
public interface DocumentVersionService {
    
    /**
     * 创建文档版本
     */
    Result<?> createVersion(Long docId, String content, String versionName, String description, Long userId);
    
    /**
     * 根据文档ID获取版本列表
     */
    Result<List<DocumentVersion>> getVersionsByDocId(Long docId);
    
    /**
     * 获取指定版本的文档内容
     */
    Result<DocumentVersion> getVersionById(Long versionId);
    
    /**
     * 回滚文档到指定版本
     */
    Result<?> rollbackToVersion(Long docId, Long versionId, Long userId);
    
    /**
     * 删除文档版本
     */
    Result<?> deleteVersion(Long versionId);
}
