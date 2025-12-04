package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.service.DocumentVersionService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 文档版本控制器
 * 处理文档版本相关的API请求
 */
@RestController
@RequestMapping("/api/version")
public class DocumentVersionController {
    
    @Autowired
    private DocumentVersionService documentVersionService;
    
    /**
     * 创建文档版本
     * POST /api/version/create
     */
    @PostMapping("/create")
    public Result<?> createVersion(@RequestParam Long docId,
                                  @RequestParam String content,
                                  @RequestParam(required = false) String versionName,
                                  @RequestParam(required = false) String description,
                                  HttpServletRequest request) {
        
        // 从请求中获取当前用户ID
        Long userId = (Long) request.getAttribute("userId");
        
        return documentVersionService.createVersion(docId, content, versionName, description, userId);
    }
    
    /**
     * 获取文档版本列表
     * GET /api/version/list/{docId}
     */
    @GetMapping("/list/{docId}")
    public Result<?> getVersions(@PathVariable Long docId) {
        return documentVersionService.getVersionsByDocId(docId);
    }
    
    /**
     * 获取指定版本的文档内容
     * GET /api/version/{versionId}
     */
    @GetMapping("/{versionId}")
    public Result<?> getVersion(@PathVariable Long versionId) {
        return documentVersionService.getVersionById(versionId);
    }
    
    /**
     * 回滚文档到指定版本
     * POST /api/version/rollback
     */
    @PostMapping("/rollback")
    public Result<?> rollbackToVersion(@RequestParam Long docId,
                                      @RequestParam Long versionId,
                                      HttpServletRequest request) {
        
        // 从请求中获取当前用户ID
        Long userId = (Long) request.getAttribute("userId");
        
        return documentVersionService.rollbackToVersion(docId, versionId, userId);
    }
    
    /**
     * 删除文档版本
     * DELETE /api/version/{versionId}
     */
    @DeleteMapping("/{versionId}")
    public Result<?> deleteVersion(@PathVariable Long versionId) {
        return documentVersionService.deleteVersion(versionId);
    }
}
