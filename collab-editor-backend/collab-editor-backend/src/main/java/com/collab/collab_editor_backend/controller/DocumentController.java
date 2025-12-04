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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/doc")
public class DocumentController {

    // 依赖注入DocumentService
    @Resource
    private DocumentService documentService;

    // 依赖注入JwtUtil
    @Resource
    private JwtUtil jwtUtil;

    /**
     * 文档列表接口
     */
    @GetMapping("/list")
    public Result<?> getDocList(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.getList(userId);
    }

    /**
     * 创建文档接口
     */
    @PostMapping("/create")
    public Result<?> createDocument(@RequestBody DocCreateDTO dto, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.create(dto, userId);
    }

    /**
     * 获取文档内容接口
     */
    @GetMapping("/content/{docId}")
    public Result<String> getDocContent(@PathVariable Long docId) {
        return documentService.getContent(docId);
    }

    /**
     * 保存文档内容接口
     */
    @PostMapping("/save")
    public Result<?> saveDocContent(@RequestBody Map<String, Object> params) {
        Long docId = Long.valueOf(params.get("docId").toString());
        String content = params.get("content").toString();
        return documentService.saveContent(docId, content);
    }

    /**
     * 导入Word文档接口
     */
    @PostMapping("/import/word")
    public Result<?> importWord(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.importWord(file, userId);
    }

    /**
     * 导入PDF文档接口
     */
    @PostMapping("/import/pdf")
    public Result<?> importPdf(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Long userId = jwtUtil.getUserIdFromToken(authorization);
        return documentService.importPdf(file, userId);
    }

    /**
     * 导出Word文档接口
     */
    @GetMapping("/export/word/{docId}")
    public void exportWord(@PathVariable Long docId, HttpServletResponse response) {
        Result<?> result = documentService.exportWord(docId);
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
    public void exportPdf(@PathVariable Long docId, @RequestBody Map<String, Object> params, HttpServletResponse response) {
        String content = (String) params.get("content");
        Result<?> result = documentService.exportPdf(docId, content);
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
}