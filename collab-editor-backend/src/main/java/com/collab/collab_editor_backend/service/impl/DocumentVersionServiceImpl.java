package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.entity.Document;
import com.collab.collab_editor_backend.entity.DocumentVersion;
import com.collab.collab_editor_backend.mapper.DocumentMapper;
import com.collab.collab_editor_backend.mapper.DocumentVersionMapper;
import com.collab.collab_editor_backend.service.DocPermissionService;
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
    
    @Autowired
    private DocPermissionService docPermissionService;
    
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
            
            // 检查用户是否有编辑权限（编辑者或管理员）
            if (!docPermissionService.hasEditPermission(docId, userId) && !docPermissionService.hasAdminPermission(docId, userId)) {
                return Result.error("您没有权限创建文档版本");
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
            
            documentVersionMapper.insertVersion(version);
            
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
            
            // 检查用户是否有查看权限
            if (!docPermissionService.hasViewPermission(docId, document.getOwnerId())) {
                return Result.error("您没有权限查看此文档的版本");
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
            
            // 检查用户是否有查看权限
            if (!docPermissionService.hasViewPermission(version.getDocId(), version.getCreatedBy())) {
                return Result.error("您没有权限查看此版本的内容");
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
            
            // 3. 检查用户是否有编辑权限（编辑者或管理员）
            if (!docPermissionService.hasEditPermission(docId, userId) && !docPermissionService.hasAdminPermission(docId, userId)) {
                return Result.error("您没有权限回滚文档版本");
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
            // 获取要删除的版本信息
            DocumentVersion version = documentVersionMapper.selectById(versionId);
            if (version == null) {
                return Result.error("版本不存在或已被删除");
            }
            
            // 检查用户是否有管理员权限
            if (!docPermissionService.hasAdminPermission(version.getDocId(), version.getCreatedBy())) {
                return Result.error("您没有权限删除文档版本");
            }
            
            // 删除版本
            int result = documentVersionMapper.deleteById(versionId);
            if (result == 0) {
                return Result.error("版本不存在或已被删除");
            }
            
            return Result.success("版本删除成功");
        } catch (Exception e) {
            return Result.error("版本删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 锁定/解锁文档版本
     */
    @Override
    public Result<?> lockVersion(Long versionId, Boolean isLocked, Long userId) {
        try {
            // 1. 获取版本信息
            DocumentVersion version = documentVersionMapper.selectById(versionId);
            if (version == null) {
                return Result.error("版本不存在");
            }
            
            // 2. 检查用户是否有权限（管理员或所有者）
            // 这里简化处理，假设只有管理员可以锁定
            if (!docPermissionService.hasAdminPermission(version.getDocId(), userId)) {
                return Result.error("您没有权限锁定/解锁此版本");
            }
            
            // 3. 更新锁定状态
            version.setIsLocked(isLocked);
            documentVersionMapper.updateById(version);
            
            return Result.success("版本" + (isLocked ? "锁定" : "解锁") + "成功");
        } catch (Exception e) {
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 比较两个版本内容的差异
     * 这里使用简单的行比较算法，实际生产中可以使用google-diff-match-patch库
     */
    @Override
    public Result<java.util.Map<String, Object>> getVersionDiff(Long versionId1, Long versionId2) {
        try {
            // 获取两个版本的内容
            DocumentVersion version1 = documentVersionMapper.selectById(versionId1);
            DocumentVersion version2 = documentVersionMapper.selectById(versionId2);
            
            if (version1 == null) {
                return Result.error("版本1不存在或已被删除");
            }
            
            if (version2 == null) {
                return Result.error("版本2不存在或已被删除");
            }
            
            if (!version1.getDocId().equals(version2.getDocId())) {
                return Result.error("两个版本不属于同一个文档");
            }
            
            // 执行文本差异比较
            String content1 = version1.getContent() != null ? version1.getContent() : "";
            String content2 = version2.getContent() != null ? version2.getContent() : "";
            
            // 将内容转换为行列表
            java.util.List<String> lines1 = java.util.Arrays.asList(content1.split("\n"));
            java.util.List<String> lines2 = java.util.Arrays.asList(content2.split("\n"));
            
            // 暂时使用简单的文本比较替代diffutils库
            // 后续可以考虑使用其他差异比较库或修复diffutils的依赖问题
            java.util.List<String> diffRows = new java.util.ArrayList<>();
            int maxLines = Math.max(lines1.size(), lines2.size());
            for (int i = 0; i < maxLines; i++) {
                String line1 = i < lines1.size() ? lines1.get(i) : "";
                String line2 = i < lines2.size() ? lines2.get(i) : "";
                if (!line1.equals(line2)) {
                    diffRows.add("- " + line1);
                    diffRows.add("+ " + line2);
                } else {
                    diffRows.add("  " + line1);
                }
            }
            
            // 构建差异结果
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("version1", version1);
            result.put("version2", version2);
            result.put("diffRows", diffRows);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取版本差异失败：" + e.getMessage());
        }
    }
}
