package com.collab.collab_editor_backend.controller;
// 替换原Resource导入
import jakarta.annotation.Resource;
// 替换原HttpServletRequest导入
import jakarta.servlet.http.HttpServletRequest;
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
    public Result<?> saveDocContent(@RequestParam Long docId, @RequestParam String content) {
        return documentService.saveContent(docId, content);
    }
}