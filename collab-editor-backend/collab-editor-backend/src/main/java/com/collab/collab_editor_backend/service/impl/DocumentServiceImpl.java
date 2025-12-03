package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.dto.DocCreateDTO;
import com.collab.collab_editor_backend.entity.Document;
import com.collab.collab_editor_backend.mapper.DocumentMapper;
import com.collab.collab_editor_backend.service.DocumentService;
import com.collab.collab_editor_backend.util.MinIOUtil;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {

    // 注入DocumentMapper（数据库操作）
    @Autowired
    private DocumentMapper documentMapper;

    // 注入MinIOUtil（文件存储操作）
    @Autowired
    private MinIOUtil minioUtil;

    /**
     * 创建文档
     * @param dto 文档创建参数（标题）
     * @param userId 文档所有者ID（当前登录用户）
     */
    @Override
    public Result<?> create(DocCreateDTO dto, Long userId) {
        try {
            // 1. 构建文档实体
            Document document = new Document();
            document.setTitle(dto.getTitle()); // 设置文档标题
            document.setOwnerId(userId); // 设置所有者ID

            // 2. 上传空内容到MinIO，获取存储路径（key）
            String minioKey = minioUtil.uploadDocContent(""); // 初始为空内容
            document.setContent(minioKey); // 存储MinIO路径

            // 3. 保存文档信息到数据库
            documentMapper.insert(document);

            return Result.success(document); // 返回创建的文档信息
        } catch (Exception e) {
            // 捕获MinIO或数据库操作异常，返回错误信息
            return Result.error("文档创建失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的文档列表
     * @param userId 用户ID（当前登录用户）
     */
    @Override
    public Result<?> getList(Long userId) {
        // 构建查询条件：查询所有者为当前用户的所有文档
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getOwnerId, userId);

        // 执行查询
        List<Document> documentList = documentMapper.selectList(queryWrapper);

        return Result.success(documentList); // 返回文档列表
    }

    /**
     * 获取文档内容（从MinIO下载）
     * @param docId 文档ID
     */
    @Override
    public Result<String> getContent(Long docId) {
        try {
            // 1. 查询文档信息（验证文档是否存在）
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }

            // 2. 从MinIO下载文档内容
            String content = minioUtil.downloadDocContent(document.getContent());

            return Result.success(content); // 返回文档内容
        } catch (Exception e) {
            return Result.error("获取文档内容失败：" + e.getMessage());
        }
    }

    /**
     * 保存文档内容（上传到MinIO）
     * @param docId 文档ID
     * @param content 最新的文档内容
     */
    @Override
    public Result<?> saveContent(Long docId, String content) {
        try {
            // 1. 验证文档是否存在
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }

            // 2. 上传最新内容到MinIO（覆盖原有内容）
            minioUtil.uploadDocContent(content);

            return Result.success("文档内容保存成功");
        } catch (Exception e) {
            return Result.error("保存文档内容失败：" + e.getMessage());
        }
    }
}