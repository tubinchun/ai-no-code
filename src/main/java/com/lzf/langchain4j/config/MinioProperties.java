package com.lzf.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO 服务地址，例如 http://8.136.9.247:9000
     */
    private String endpoint;

    /**
     * 外部访问地址；为空时使用 endpoint
     */
    private String publicEndpoint;

    /**
     * Access Key
     */
    private String accessKey;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 默认 bucket
     */
    private String bucketName;
}
