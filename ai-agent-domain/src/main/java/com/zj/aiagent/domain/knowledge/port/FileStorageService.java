package com.zj.aiagent.domain.knowledge.port;

import java.io.InputStream;

/**
 * 文件存储服务端口（Port）
 * 用于对接对象存储服务（如 MinIO、OSS、S3）
 * 
 * Infrastructure 层实现该接口
 */
public interface FileStorageService {
    /**
     * 上传文件
     * 
     * @param bucketName  存储桶名称
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件输入流
     * @param size        文件大小（字节）
     * @return 文件访问 URL
     */
    String upload(String bucketName, String objectName, InputStream inputStream, long size);

    /**
     * 下载文件
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     * @return 文件输入流
     */
    InputStream download(String bucketName, String objectName);

    /**
     * 删除文件
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    void delete(String bucketName, String objectName);
}
