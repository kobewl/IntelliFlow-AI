package com.kobeai.hub.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@Data
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            MinioClient client = minioClient();

            // 检查存储桶是否存在
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // 创建存储桶
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建存储桶成功: {}", bucketName);
            } else {
                log.info("存储桶已存在: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化MinIO失败: {}", e.getMessage(), e);
            // 这里我们不抛出异常，因为这可能不是致命错误
        }
    }
}