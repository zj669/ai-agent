package com.zj.aiagent.infrastructure.knowledge;

import com.zj.aiagent.domain.knowledge.port.FileStorageService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * MinIO 文件存储服务实现
 * 实现 Domain 层的 FileStorageService 接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOFileStorageService implements FileStorageService {

    private final MinioClient minioClient;

    @Override
    public String upload(String bucketName, String objectName, InputStream inputStream, long size) {
        try {
            // 确保 Bucket 存在
            ensureBucketExists(bucketName);

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .build());

            // 返回文件存储路径标识
            String fileUrl = String.format("%s/%s", bucketName, objectName);

            log.info("Successfully uploaded file to MinIO: {}", fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("Failed to upload file to MinIO: bucket={}, object={}",
                    bucketName, objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: bucket={}, object={}",
                    bucketName, objectName, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());

            log.info("Successfully deleted file from MinIO: bucket={}, object={}",
                    bucketName, objectName);

        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: bucket={}, object={}",
                    bucketName, objectName, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 确保 Bucket 存在，不存在则创建
     * 
     * @param bucketName Bucket 名称
     */
    private void ensureBucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build());

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build());
                log.info("Created MinIO bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket exists: {}", bucketName, e);
            throw new RuntimeException("Bucket 创建失败: " + e.getMessage(), e);
        }
    }
}
