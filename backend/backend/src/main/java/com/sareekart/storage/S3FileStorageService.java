package com.sareekart.storage;

import com.sareekart.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * S3-backed storage for production. Object keys are immutable/versioned:
 * products/{productId}/{uuid}.{ext} — suitable for CDN long-caching.
 *
 * Public URL strategy: https://{bucket}.s3.{region}.amazonaws.com/{key}
 * (or CDN base URL if configured). Bucket must be read-only for public
 * access; write access is server-side only via IAM.
 */
@Slf4j
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final FileStorageProperties properties;
    private final ImageValidator validator;

    @Override
    public String store(Long productId, MultipartFile file) {
        validator.validate(file);
        String ext = validator.extensionFor(file.getContentType());
        String key = "products/" + productId + "/" + UUID.randomUUID() + "." + ext;

        try (var is = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .cacheControl("public, max-age=31536000, immutable")
                .build();
            s3Client.putObject(request, RequestBody.fromInputStream(is, file.getSize()));
        } catch (IOException e) {
            log.error("S3 upload failed for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("File storage failed", e);
        }

        log.info("Stored in S3: s3://{}/{}", properties.getBucket(), key);
        return publicUrl(key);
    }

    @Override
    public void delete(String url) {
        String key = extractKey(url);
        if (key == null) return;
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .build());
            log.info("Deleted from S3: s3://{}/{}", properties.getBucket(), key);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", key, e.getMessage());
        }
    }

    @Override
    public String extractKey(String url) {
        if (url == null) return null;
        String cdnBase = properties.getCdnBaseUrl();
        if (!cdnBase.isBlank() && url.startsWith(cdnBase)) {
            return url.substring(cdnBase.length() + 1); // +1 for '/'
        }
        String s3UrlPrefix = "https://" + properties.getBucket() + ".s3."
            + properties.getRegion() + ".amazonaws.com/";
        if (url.startsWith(s3UrlPrefix)) {
            return url.substring(s3UrlPrefix.length());
        }
        // Also match virtual-hosted-style with any region
        if (url.contains(".s3.") && url.contains(".amazonaws.com/")) {
            int idx = url.indexOf(".amazonaws.com/");
            if (idx > 0) return url.substring(idx + ".amazonaws.com/".length());
        }
        return null;
    }

    private String publicUrl(String key) {
        String cdn = properties.getCdnBaseUrl();
        if (!cdn.isBlank()) {
            return cdn + "/" + key;
        }
        return "https://" + properties.getBucket() + ".s3." + properties.getRegion()
            + ".amazonaws.com/" + key;
    }
}