package com.kobeai.hub.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 上传文件到 MinIO
     * 
     * @param file       文件
     * @param bucketName 存储桶名称
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String bucketName) throws Exception;

    /**
     * 删除文件
     * 
     * @param bucketName 存储桶名称
     * @param objectName 对象名称
     */
    void deleteFile(String bucketName, String objectName) throws Exception;
}