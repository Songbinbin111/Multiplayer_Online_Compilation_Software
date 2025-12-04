package com.collab.collab_editor_backend.config;

// 暂时注释MinIO配置，以便应用能正常启动
/*
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfig {

    // 这里的 @Value("${minio.endpoint}") 必须和配置文件中的 key 完全一致
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        // 初始化客户端，使用配置的 endpoint
        return MinioClient.builder()
                .endpoint(endpoint)  // 这里会使用上面读取的正确地址
                .credentials(accessKey, secretKey)
                .build();
    }
}
*/