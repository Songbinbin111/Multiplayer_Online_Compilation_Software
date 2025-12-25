package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.collab_editor_backend.entity.DocPermission;
import com.collab.collab_editor_backend.entity.Document;
import com.collab.collab_editor_backend.mapper.DocPermissionMapper;
import com.collab.collab_editor_backend.mapper.DocumentMapper;
import com.collab.collab_editor_backend.service.DocPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档权限模块服务层实现类
 */
@Service
public class DocPermissionServiceImpl implements DocPermissionService {

    @Autowired
    private DocPermissionMapper docPermissionMapper;

    @Autowired
    private DocumentMapper documentMapper;

    /**
     * 为用户分配文档权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @param permissionType 权限类型（0-查看，1-编辑）
     * @return 创建的权限记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocPermission assignPermission(Long docId, Long userId, Integer permissionType) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        // 检查权限类型是否合法
        if (permissionType != 0 && permissionType != 1 && permissionType != 2) {
            throw new RuntimeException("权限类型不合法");
        }

        // 检查是否已存在权限记录
        DocPermission existingPermission = docPermissionMapper.getPermissionByDocIdAndUserId(docId, userId);
        if (existingPermission != null) {
            // 已存在权限，更新权限类型
            existingPermission.setPermissionType(permissionType);
            docPermissionMapper.updateById(existingPermission);
            return existingPermission;
        }

        // 创建新的权限记录
        DocPermission permission = new DocPermission();
        permission.setDocId(docId);
        permission.setUserId(userId);
        permission.setPermissionType(permissionType);
        permission.setCreateTime(LocalDateTime.now());
        docPermissionMapper.insert(permission);
        return permission;
    }

    /**
     * 移除用户的文档权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 是否移除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePermission(Long docId, Long userId) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        // 检查是否是文档所有者
        if (document.getOwnerId().equals(userId)) {
            throw new RuntimeException("文档所有者不能移除自己的权限");
        }

        // 移除权限
        LambdaQueryWrapper<DocPermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocPermission::getDocId, docId)
                .eq(DocPermission::getUserId, userId);
        return docPermissionMapper.delete(queryWrapper) > 0;
    }

    /**
     * 更新用户的文档权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @param permissionType 新的权限类型（0-查看，1-编辑）
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePermission(Long docId, Long userId, Integer permissionType) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        // 检查权限类型是否合法
        if (permissionType != 0 && permissionType != 1 && permissionType != 2) {
            throw new RuntimeException("权限类型不合法");
        }

        // 检查权限记录是否存在
        DocPermission permission = docPermissionMapper.getPermissionByDocIdAndUserId(docId, userId);
        if (permission == null) {
            return false;
        }

        // 更新权限类型
        permission.setPermissionType(permissionType);
        return docPermissionMapper.updateById(permission) > 0;
    }

    /**
     * 根据文档ID获取所有权限记录
     * @param docId 文档ID
     * @return 权限记录列表
     */
    @Override
    public List<DocPermission> getPermissionsByDocId(Long docId) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        return docPermissionMapper.getPermissionsByDocId(docId);
    }

    /**
     * 获取用户在指定文档的权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 权限记录，null表示无权限
     */
    @Override
    public DocPermission getPermissionByDocIdAndUserId(Long docId, Long userId) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        // 文档所有者默认拥有所有权限
        if (document.getOwnerId().equals(userId)) {
            DocPermission ownerPermission = new DocPermission();
            ownerPermission.setDocId(docId);
            ownerPermission.setUserId(userId);
            ownerPermission.setPermissionType(2); // 所有者拥有管理员权限
            ownerPermission.setCreateTime(LocalDateTime.now());
            return ownerPermission;
        }

        // 普通用户的权限
        return docPermissionMapper.getPermissionByDocIdAndUserId(docId, userId);
    }

    /**
     * 检查用户是否有指定文档的查看权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 是否有查看权限
     */
    @Override
    public boolean hasViewPermission(Long docId, Long userId) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            return false;
        }

        // 文档所有者默认有查看权限
        if (document.getOwnerId().equals(userId)) {
            return true;
        }

        // 检查是否有查看或编辑权限
        DocPermission permission = docPermissionMapper.getPermissionByDocIdAndUserId(docId, userId);
        return permission != null;
    }

    /**
     * 检查用户是否有指定文档的编辑权限
     * @param docId 文档ID
     * @param userId 用户ID
     * @return 是否有编辑权限
     */
    @Override
    public boolean hasEditPermission(Long docId, Long userId) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            return false;
        }

        // 文档所有者默认有编辑权限
        if (document.getOwnerId().equals(userId)) {
            return true;
        }

        // 检查是否有编辑权限
        DocPermission permission = docPermissionMapper.getPermissionByDocIdAndUserId(docId, userId);
        return permission != null && permission.getPermissionType() == 1;
    }

    /**
     * 获取用户有权限的所有文档ID列表
     * @param userId 用户ID
     * @param permissionType 权限类型（可选，null表示所有权限）
     * @return 文档ID列表
     */
    @Override
    public boolean hasAdminPermission(Long docId, Long userId) {
        // 检查文档是否存在
        Document document = documentMapper.selectById(docId);
        if (document == null) {
            return false;
        }

        // 文档所有者默认有管理员权限
        if (document.getOwnerId().equals(userId)) {
            return true;
        }

        // 检查是否有管理员权限
        DocPermission permission = docPermissionMapper.getPermissionByDocIdAndUserId(docId, userId);
        return permission != null && permission.getPermissionType() == 2;
    }

    @Override
    public List<Long> getDocIdsByUserId(Long userId, Integer permissionType) {
        return docPermissionMapper.getDocIdsByUserId(userId, permissionType);
    }
}
