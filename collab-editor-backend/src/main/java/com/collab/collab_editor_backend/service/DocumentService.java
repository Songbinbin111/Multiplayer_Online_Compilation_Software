package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.dto.DocCreateDTO;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;

/**
 * 文档服务接口（与DocumentServiceImpl的实现方法匹配）
 */
public interface DocumentService {
    // 创建文档
    Result<?> create(DocCreateDTO dto, Long userId);
    // 查询用户的文档列表
    Result<?> getList(Long userId);
    // 根据分类查询文档列表
    Result<?> getListByCategory(Long userId, String category);
    // 获取用户的所有文档分类
    Result<?> getCategories(Long userId);
    // 获取文档内容（从MinIO下载）
    Result<String> getContent(Long docId, Long userId);
    // 保存文档内容（上传到MinIO）
    Result<?> saveContent(Long docId, String content, Long userId);
    // 导入Word文档
    Result<?> importWord(MultipartFile file, Long userId, String category);
    // 导入PDF文档
    Result<?> importPdf(MultipartFile file, Long userId, String category);
    // 导出为Word文档
    Result<?> exportWord(Long docId, Long userId);
    // 导出为PDF文档
    Result<?> exportPdf(Long docId, String content, Long userId);
    // 搜索文档
    Result<?> search(Long userId, String keyword, String tags, String author, LocalDateTime startTime, LocalDateTime endTime, String sortField, String sortOrder);
    // 删除文档
    Result<?> deleteDocument(Long docId, Long userId);
}