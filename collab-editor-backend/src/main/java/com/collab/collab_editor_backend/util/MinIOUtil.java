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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MinIO 工具类：适配本地 MinIO 环境，封装文件上传、删除操作
 */
@Component
public class MinIOUtil {

    private static final Logger logger = LoggerFactory.getLogger(MinIOUtil.class);

    @Autowired
    private MinioClient minioClient; // 由 MinIOConfig 注入的客户端

    // 从配置文件读取 MinIO 存储桶名称
    @Value("${minio.bucketName}")
    private String bucketName;

    // 从配置文件读取 MinIO 服务地址（用于拼接公开访问 URL）
    @Value("${minio.endpoint}")
    private String minioEndpoint;

    // 本地存储路径（用于MinIO不可用时的备选方案）
    private static final String LOCAL_STORAGE_PATH = "uploads/";
    // 本地文件访问前缀
    private static final String LOCAL_FILE_PREFIX = "/uploads/";

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

        // 先尝试使用MinIO上传
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uniqueFileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            // 处理 endpoint 斜杠问题，确保 URL 格式正确
            String normalizedEndpoint = minioEndpoint.endsWith("/") ? minioEndpoint.substring(0, minioEndpoint.length() - 1) : minioEndpoint;
            logger.info("文件通过MinIO上传成功: {}", normalizedEndpoint + "/" + bucketName + "/" + uniqueFileName);
            return normalizedEndpoint + "/" + bucketName + "/" + uniqueFileName;
        } catch (Exception e) {
            logger.warn("MinIO上传失败，将使用本地文件存储: {}", e.getMessage());
            
            // MinIO上传失败，使用本地文件存储
            return uploadFileToLocal(file, uniqueFileName);
        }
    }
    
    /**
     * 将文件上传到本地存储
     * @param file 前端上传的文件
     * @param uniqueFileName 唯一文件名
     * @return 本地文件访问URL
     */
    private String uploadFileToLocal(MultipartFile file, String uniqueFileName) throws IOException {
        // 创建上传目录（如果不存在）
        File uploadDir = new File(LOCAL_STORAGE_PATH);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // 保存文件到本地
        Path filePath = Paths.get(LOCAL_STORAGE_PATH + uniqueFileName);
        Files.write(filePath, file.getBytes());
        
        logger.info("文件通过本地存储上传成功: {}", LOCAL_FILE_PREFIX + uniqueFileName);
        return LOCAL_FILE_PREFIX + uniqueFileName;
    }

    /**
     * 删除文件（支持MinIO和本地存储）
     * @param fileName 要删除的文件名或URL
     */
    public void deleteFile(String fileName) throws MinioException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        try {
            // 判断是MinIO文件还是本地文件
            if (fileName.startsWith(LOCAL_FILE_PREFIX)) {
                // 删除本地文件
                String localFileName = fileName.substring(LOCAL_FILE_PREFIX.length());
                Path filePath = Paths.get(LOCAL_STORAGE_PATH + localFileName);
                Files.deleteIfExists(filePath);
                logger.info("本地文件删除成功: {}", fileName);
            } else {
                // 删除MinIO文件
                // 从URL中提取文件名
                String minioFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(minioFileName)
                                .build()
                );
                logger.info("MinIO文件删除成功: {}", fileName);
            }
        } catch (Exception e) {
            logger.warn("文件删除失败: {}", e.getMessage());
            // 不抛出异常，避免因删除失败影响主流程
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