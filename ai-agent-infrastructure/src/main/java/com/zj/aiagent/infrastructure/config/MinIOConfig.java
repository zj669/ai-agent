package com.zj.aiagent.infrastructure.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 * 提供 MinioClient Bean 供文件存储服务使用
 */
@Slf4j
@Configuration
public class MinIOConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    /**
     * 配置 MinioClient Bean
     * 
     * @return MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        log.info("Initializing MinioClient: endpoint={}", endpoint);

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
