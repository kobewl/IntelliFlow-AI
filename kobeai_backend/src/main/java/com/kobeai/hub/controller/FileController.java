package com.kobeai.hub.controller;

import com.kobeai.hub.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.kobeai.hub.constant.FileConstant.COS_HOST;


/**
 * 用户图片上传接口
 *
 * @author dongyanmian
 */
@RestController
@RequestMapping("/file")
@Slf4j
@Api(tags = "用户图片上传接口")
public class FileController {


    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 上传文件
     *
     * @param data 文件
     * @return 完整url
     */
    @PostMapping("/upload")
    @ApiOperation(value = "上传图片")
    public String upload(@RequestParam("data") MultipartFile data) throws Exception {
        // 检查文件是否为空
        if (data.isEmpty()) {
            return "文件为空";
        }

        // 检查文件类型
        String contentType = data.getContentType();
        Set<String> allowedTypes = new HashSet<>(Arrays.asList("image/jpeg", "image/png", "application/pdf"));
        if (!allowedTypes.contains(contentType)) {
            return "不支持的文件类型";
        }

        if (!allowedTypes.contains(contentType)) {
            return "不支持的文件类型";
        }

        // 检查文件大小
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (data.getSize() > maxSize) {
            return "文件大小超过限制";
        }
        // 在第一次使用时初始化 MinioClient
        if (minioClient == null) {
            minioClient = minioConfig.minioClient();
        }

        log.info("文件上传开始");

        // 生成唯一文件名
        String originalFileName = data.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = generateUniqueFileName() + fileExtension;

        InputStream inputStream = data.getInputStream();
        minioClient.putObject(PutObjectArgs.builder().bucket(minioConfig.getBucketName()).object(uniqueFileName).stream(inputStream, data.getSize(), -1).contentType(data.getContentType()).build());

        // 构建完整的 URL
        String baseUrl = COS_HOST;
        String fullUrl = baseUrl + uniqueFileName;

        log.info("文件上传成功，完整 URL: {}", fullUrl);
        return fullUrl; // 返回完整的 URL
    }

    /**
     * 生成唯一文件名
     *
     * @return 唯一文件名
     */
    private String generateUniqueFileName() {
        return System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

}
