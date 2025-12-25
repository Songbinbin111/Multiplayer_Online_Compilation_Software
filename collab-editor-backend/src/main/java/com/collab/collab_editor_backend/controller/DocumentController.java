package com.collab.collab_editor_backend.controller;
// 替换原Resource导入
import jakarta.annotation.Resource;
// 替换原HttpServletRequest导入
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// 1. 导入DocumentService接口
import com.collab.collab_editor_backend.service.DocumentService;
// 2. 导入DTO类
import com.collab.collab_editor_backend.dto.DocCreateDTO;
// 3. 导入JwtUtil工具类
import com.collab.collab_editor_backend.util.JwtUtil;
// 4. 导入统一响应类
import com.collab.collab_editor_backend.util.Result;

// 7. 导入Spring REST接口注解
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import com.collab.collab_editor_backend.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import com.collab.collab_editor_backend.entity.Document;

@RestController
@RequestMapping("/api/doc")
public class DocumentController {

    // 依赖注入DocumentService
    @Resource
    private DocumentService documentService;

    // 依赖注入JwtUtil
    @Resource
    private JwtUtil jwtUtil;
    
    // 依赖注入UserActivityService
    @Autowired
    private UserActivityService userActivityService;

    /**
     * 文档列表接口
     */
    @GetMapping("/list")
    public Result<?> getDocList(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        return documentService.getList(userId);
    }

    /**
     * 按分类获取文档列表接口
     */
    @GetMapping("/list/category/{category}")
    public Result<?> getDocListByCategory(@RequestHeader("Authorization") String token, @PathVariable String category) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        return documentService.getListByCategory(userId, category);
    }

    /**
     * 获取所有分类接口
     */
    @GetMapping("/categories")
    public Result<?> getCategories(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        return documentService.getCategories(userId);
    }

    /**
     * 删除文档接口
     */
    @DeleteMapping("/{docId}")
    public Result<?> deleteDocument(@PathVariable Long docId, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.deleteDocument(docId, userId);
    }

    /**
     * 创建文档接口
     */
    @PostMapping("/create")
    public Result<?> createDocument(@RequestBody DocCreateDTO dto, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        
        Result<?> result = documentService.create(dto, userId);
        
        // 记录创建文档行为
        if (result.getCode() == 200 && result.getData() != null) {
            Document doc = (Document) result.getData();
            Long docId = doc.getId();
            userActivityService.recordActivityWithObject(
                userId, 
                "create_document", 
                docId, 
                "document", 
                "创建文档：" + dto.getTitle()
            );
        }
        
        return result;
    }

    /**
     * 获取文档内容接口
     */
    @GetMapping("/content/{docId}")
    public Result<String> getDocContent(@PathVariable Long docId, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.getContent(docId, userId);
    }

    /**
     * 保存文档内容接口
     */
    @PostMapping("/save")
    public Result<?> saveDocContent(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long docId = Long.valueOf(params.get("docId").toString());
        String content = params.get("content").toString();
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        
        Result<?> result = documentService.saveContent(docId, content, userId);
        
        // 记录保存文档行为
        if (result.getCode() == 200) {
            userActivityService.recordActivityWithObject(
                userId, 
                "save_document", 
                docId, 
                "document", 
                "保存文档内容"
            );
        }
        
        return result;
    }

    /**
     * 导入Word文档接口
     */
    @PostMapping("/import/word")
    public Result<?> importWord(@RequestParam("file") MultipartFile file,
                               @RequestParam(required = false) String category,
                               HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.importWord(file, userId, category);
    }

    /**
     * 导入PDF文档接口
     */
    @PostMapping("/import/pdf")
    public Result<?> importPdf(@RequestParam("file") MultipartFile file,
                              @RequestParam(required = false) String category,
                              HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.importPdf(file, userId, category);
    }

    /**
     * 导出Word文档接口
     */
    @GetMapping("/export/word/{docId}")
    public void exportWord(@PathVariable Long docId, HttpServletRequest request, HttpServletResponse response) {
        String token = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Result<?> result = documentService.exportWord(docId, userId);
        if (result.getCode() == 200) {
            try {
                String fileName = result.getMessage();
                InputStream inputStream = (ByteArrayInputStream) result.getData();
                
                // 设置响应头
                response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                
                // 写入响应
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    response.getOutputStream().write(buffer, 0, len);
                }
                
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 导出PDF文档接口
     */
    @PostMapping("/export/pdf/{docId}")
    public void exportPdf(@PathVariable Long docId, @RequestBody Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) {
        String content = (String) params.get("content");
        String token = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(token);
        Result<?> result = documentService.exportPdf(docId, content, userId);
        if (result.getCode() == 200) {
            try {
                String fileName = result.getMessage();
                InputStream inputStream = (ByteArrayInputStream) result.getData();
                
                // 设置响应头
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
                
                // 写入响应
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    response.getOutputStream().write(buffer, 0, len);
                }
                
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 搜索文档接口
     */
    @GetMapping("/search")
    public Result<?> searchDocument(@RequestHeader("Authorization") String authorization,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String tags,
                                  @RequestParam(required = false) String author,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                  @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                  @RequestParam(required = false, defaultValue = "updateTime") String sortField,
                                  @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.search(userId, keyword, tags, author, startTime, endTime, sortField, sortOrder);
    }
}