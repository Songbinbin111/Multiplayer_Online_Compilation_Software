package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.entity.Document;
import com.collab.collab_editor_backend.entity.DocumentVersion;
import com.collab.collab_editor_backend.mapper.DocumentMapper;
import com.collab.collab_editor_backend.mapper.DocumentVersionMapper;
import com.collab.collab_editor_backend.service.DocumentVersionService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档版本服务实现类
 */
@Service
public class DocumentVersionServiceImpl implements DocumentVersionService {
    
    @Autowired
    private DocumentVersionMapper documentVersionMapper;
    
    @Autowired
    private DocumentMapper documentMapper;
    
    /**
     * 创建文档版本
     * 保存文档内容时自动创建新版本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> createVersion(Long docId, String content, String versionName, String description, Long userId) {
        try {
            // 验证文档是否存在
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }
            
            // 获取当前文档的最新版本号
            Integer latestVersionNumber = getLatestVersionNumber(docId);
            Integer newVersionNumber = latestVersionNumber + 1;
            
            // 创建版本实体
            DocumentVersion version = new DocumentVersion();
            version.setDocId(docId);
            version.setContent(content);
            version.setVersionName(versionName != null && !versionName.isEmpty() ? versionName : "版本 " + newVersionNumber);
            version.setDescription(description);
            version.setCreatedBy(userId);
            version.setCreatedTime(LocalDateTime.now());
            version.setVersionNumber(newVersionNumber);
            
            // 保存版本
            documentVersionMapper.insert(version);
            
            return Result.success(version);
        } catch (Exception e) {
            return Result.error("版本创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文档的最新版本号
     */
    private Integer getLatestVersionNumber(Long docId) {
        LambdaQueryWrapper<DocumentVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentVersion::getDocId, docId)
                   .orderByDesc(DocumentVersion::getVersionNumber)
                   .last("LIMIT 1");
        
        DocumentVersion latestVersion = documentVersionMapper.selectOne(queryWrapper);
        return latestVersion != null ? latestVersion.getVersionNumber() : 0;
    }
    
    /**
     * 根据文档ID获取版本列表
     */
    @Override
    public Result<List<DocumentVersion>> getVersionsByDocId(Long docId) {
        try {
            // 验证文档是否存在
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }
            
            // 获取版本列表
            List<DocumentVersion> versions = documentVersionMapper.getVersionsByDocId(docId);
            return Result.success(versions);
        } catch (Exception e) {
            return Result.error("获取版本列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取指定版本的文档内容
     */
    @Override
    public Result<DocumentVersion> getVersionById(Long versionId) {
        try {
            DocumentVersion version = documentVersionMapper.selectById(versionId);
            if (version == null) {
                return Result.error("版本不存在或已被删除");
            }
            return Result.success(version);
        } catch (Exception e) {
            return Result.error("获取版本内容失败：" + e.getMessage());
        }
    }
    
    /**
     * 回滚文档到指定版本
     * 将文档内容恢复到指定版本，并创建新的版本记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> rollbackToVersion(Long docId, Long versionId, Long userId) {
        try {
            // 1. 获取指定版本
            DocumentVersion targetVersion = documentVersionMapper.selectById(versionId);
            if (targetVersion == null || !targetVersion.getDocId().equals(docId)) {
                return Result.error("版本不存在或不属于该文档");
            }
            
            // 2. 获取当前文档
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }
            
            // 3. 保存当前文档内容为新版本（回滚前的版本）
            createVersion(docId, document.getContent(), "回滚前版本", "在回滚到版本 " + targetVersion.getVersionNumber() + " 前创建", userId);
            
            // 4. 更新文档内容为指定版本的内容
            document.setContent(targetVersion.getContent());
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            
            // 5. 创建回滚后的新版本记录
            createVersion(docId, targetVersion.getContent(), "回滚到版本 " + targetVersion.getVersionNumber(), "从版本 " + targetVersion.getVersionNumber() + " 回滚", userId);
            
            return Result.success("文档已成功回滚到指定版本");
        } catch (Exception e) {
            return Result.error("文档回滚失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除文档版本
     */
    @Override
    public Result<?> deleteVersion(Long versionId) {
        try {
            int result = documentVersionMapper.deleteById(versionId);
            if (result == 0) {
                return Result.error("版本不存在或已被删除");
            }
            return Result.success("版本删除成功");
        } catch (Exception e) {
            return Result.error("版本删除失败：" + e.getMessage());
        }
    }
}
