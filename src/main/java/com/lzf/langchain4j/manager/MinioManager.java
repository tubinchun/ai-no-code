package com.lzf.langchain4j.manager;

import cn.hutool.core.util.StrUtil;
import com.lzf.langchain4j.config.MinioProperties;
import com.lzf.langchain4j.exception.BusinessException;
import com.lzf.langchain4j.exception.ErrorCode;
import com.lzf.langchain4j.exception.ThrowUtils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class MinioManager {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioProperties minioProperties;

    public String uploadFile(MultipartFile file) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        String originalFilename = file.getOriginalFilename();
        String suffix = "";
        if (StrUtil.isNotBlank(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID() + suffix;
        return uploadFile(file, objectName);
    }

    public String uploadFile(MultipartFile file, String objectName) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(objectName), ErrorCode.PARAMS_ERROR, "文件路径不能为空");
        try (InputStream inputStream = file.getInputStream()) {
            return uploadFile(inputStream, file.getSize(), file.getContentType(), objectName);
        } catch (Exception e) {
            log.error("MinIO 文件上传失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    public String uploadFile(File file, String objectName) {
        ThrowUtils.throwIf(file == null || !file.exists() || !file.isFile(), ErrorCode.PARAMS_ERROR, "文件不存在");
        ThrowUtils.throwIf(StrUtil.isBlank(objectName), ErrorCode.PARAMS_ERROR, "文件路径不能为空");
        Path path = file.toPath();
        try (InputStream inputStream = Files.newInputStream(path)) {
            String contentType = Files.probeContentType(path);
            return uploadFile(inputStream, file.length(), contentType, objectName);
        } catch (Exception e) {
            log.error("MinIO 文件上传失败，file: {}, objectName: {}", file.getAbsolutePath(), objectName, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    public String uploadFile(String objectName, File file) {
        return uploadFile(file, objectName);
    }

    public String uploadFile(InputStream inputStream, long size, String contentType, String objectName) {
        checkConfig();
        ThrowUtils.throwIf(inputStream == null, ErrorCode.PARAMS_ERROR, "文件流不能为空");
        ThrowUtils.throwIf(size < 0, ErrorCode.PARAMS_ERROR, "文件大小异常");
        ThrowUtils.throwIf(StrUtil.isBlank(objectName), ErrorCode.PARAMS_ERROR, "文件路径不能为空");
        String bucketName = minioProperties.getBucketName();
        String normalizedObjectName = normalizeObjectName(objectName);
        try {
            createBucketIfAbsent(bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(normalizedObjectName)
                    .stream(inputStream, size, -1)
                    .contentType(StrUtil.blankToDefault(contentType, "application/octet-stream"))
                    .build());
            return buildFileUrl(bucketName, normalizedObjectName);
        } catch (Exception e) {
            log.error("MinIO 文件上传失败，bucket: {}, objectName: {}", bucketName, normalizedObjectName, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    private void createBucketIfAbsent(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        }
    }

    private void checkConfig() {
        ThrowUtils.throwIf(StrUtil.isBlank(minioProperties.getEndpoint()), ErrorCode.SYSTEM_ERROR,
                "MinIO endpoint 未配置");
        ThrowUtils.throwIf(StrUtil.isBlank(minioProperties.getAccessKey()), ErrorCode.SYSTEM_ERROR,
                "MinIO accessKey 未配置");
        ThrowUtils.throwIf(StrUtil.isBlank(minioProperties.getSecretKey()), ErrorCode.SYSTEM_ERROR,
                "MinIO secretKey 未配置");
        ThrowUtils.throwIf(StrUtil.isBlank(minioProperties.getBucketName()), ErrorCode.SYSTEM_ERROR,
                "MinIO bucketName 未配置");
    }

    private String normalizeObjectName(String objectName) {
        return objectName.replace("\\", "/").replaceAll("^/+", "");
    }

    private String buildFileUrl(String bucketName, String objectName) {
        String baseUrl = StrUtil.blankToDefault(minioProperties.getPublicEndpoint(), minioProperties.getEndpoint());
        String encodedObjectName = encodeObjectName(objectName);
        return StrUtil.removeSuffix(baseUrl, "/") + "/" + bucketName + "/" + encodedObjectName;
    }

    private String encodeObjectName(String objectName) {
        String[] parts = objectName.split("/");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = URLEncoder.encode(parts[i], StandardCharsets.UTF_8).replace("+", "%20");
        }
        return String.join("/", parts);
    }
}
