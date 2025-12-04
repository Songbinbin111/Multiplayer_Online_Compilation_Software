package com.collab.collab_editor_backend.util; // 你的包路径（必须加，对应你的util目录）

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.GetObjectArgs;
import java.util.UUID;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;

/**
 * MinIO 工具类：适配本地 MinIO 环境，封装文件上传、删除操作
 */
public class MinIOUtil {

    @Autowired
    private MinioClient minioClient; // 由 MinIOConfig 注入的客户端

    // 从配置文件读取 MinIO 存储桶名称
    @Value("${minio.bucketName}")
    private String bucketName;

    // 从配置文件读取 MinIO 服务地址（用于拼接公开访问 URL）
    @Value("${minio.endpoint}")
    private String minioEndpoint;

    /**
     * 上传文件并返回公开访问 URL
     * @param file 前端上传的文件
     * @return 可直接访问的文件 URL
     */
    public String uploadFile(MultipartFile file) throws MinioException, IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String fileSuffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileSuffix;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uniqueFileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("文件上传失败", e);
        }

        // 处理 endpoint 斜杠问题，确保 URL 格式正确
        String normalizedEndpoint = minioEndpoint.endsWith("/") ? minioEndpoint.substring(0, minioEndpoint.length() - 1) : minioEndpoint;
        return normalizedEndpoint + "/" + bucketName + "/" + uniqueFileName;
    }

    /**
     * 删除 MinIO 中的文件
     * @param fileName 要删除的文件名
     */
    public void deleteFile(String fileName) throws MinioException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("文件删除失败", e);
        }
    }

    /**
     * 上传文档字符串内容到 MinIO
     */
    public String uploadDocContent(String content) throws MinioException, UnsupportedEncodingException {
        if (content == null) {
            content = ""; // 处理空内容
        }
        String uniqueFileName = "docs/" + UUID.randomUUID().toString() + ".txt";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8); // 明确编码

        try (InputStream inputStream = new ByteArrayInputStream(contentBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uniqueFileName)
                            .stream(inputStream, contentBytes.length, -1) // 用实际字节长度替代 available()
                            .contentType("text/plain;charset=UTF-8") // 明确字符集
                            .build()
            );
        } catch (Exception e) {
            throw new MinioException("文档内容上传失败", e);
        }

        return uniqueFileName;
    }

    /**
     * 从 MinIO 下载文档字符串内容
     */
    public String downloadDocContent(String key) throws MinioException, IOException {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("文件key不能为空");
        }
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(key)
                        .build()
        )) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8); // 明确编码
        } catch (Exception e) {
            throw new MinioException("文档内容下载失败", e);
        }
    }

    // 自定义异常类，便于统一处理
    public static class MinioException extends Exception {
        public MinioException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}