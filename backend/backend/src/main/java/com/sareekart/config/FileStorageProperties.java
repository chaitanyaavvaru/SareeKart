package com.sareekart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {

    public enum Provider { LOCAL, S3 }

    private Provider provider = Provider.LOCAL;

    // S3 settings
    private String bucket = "";
    private String region = "us-east-1";
    private String endpoint = ""; // optional: for MinIO/LocalStack

    // Local settings
    private String uploadDir = "./uploads/products";

    /** Max upload size in bytes (5 MB default). */
    private long maxFileSize = 5 * 1024 * 1024;

    private Set<String> allowedContentTypes = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    /** CDN base URL prepended to S3 keys when set (e.g. CloudFront domain). */
    private String cdnBaseUrl = "";
}
