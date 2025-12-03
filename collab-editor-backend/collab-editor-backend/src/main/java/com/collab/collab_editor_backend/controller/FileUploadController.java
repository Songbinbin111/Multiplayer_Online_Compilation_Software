package com.collab.collab_editor_backend.controller; // 包路径必须正确，否则Spring扫描不到

import com.collab.collab_editor_backend.util.MinIOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

// 1. @RestController：标识这是一个接口类（返回JSON数据，不跳转页面）
@RestController
public class FileUploadController {

    // 2. @Autowired：自动注入MinIOUtil（之前写的MinIO工具类，无需手动new）
    @Autowired
    private MinIOUtil minIOUtil;

    // 3. @PostMapping("/upload")：定义POST请求接口，路径为“/upload”（测试时要访问这个路径）
    @PostMapping("/upload")
    // 4. @RequestParam("file")：接收前端传来的“file”参数（文件数据）
    public String uploadTest(@RequestParam("file") MultipartFile file) {
        try {
            // 5. 调用MinIOUtil的上传方法，返回文件访问URL，直接返回给前端
            return minIOUtil.uploadFile(file);
        } catch (Exception e) {
            // 若上传失败，返回错误信息
            return "上传失败：" + e.getMessage();
        }
    }
}