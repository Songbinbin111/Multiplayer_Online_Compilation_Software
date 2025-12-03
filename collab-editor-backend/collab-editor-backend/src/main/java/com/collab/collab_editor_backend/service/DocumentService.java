package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.dto.DocCreateDTO;
import com.collab.collab_editor_backend.util.Result;

public interface DocumentService {
    // 创建文档
    Result<?> create(DocCreateDTO dto, Long userId);
    // 查询用户的文档列表
    Result<?> getList(Long userId);
    // 获取文档内容（从 MinIO 下载）
    Result<String> getContent(Long docId);
    // 保存文档内容（上传到 MinIO）
    Result<?> saveContent(Long docId, String content);
}