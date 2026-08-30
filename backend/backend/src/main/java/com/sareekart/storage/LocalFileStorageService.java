package com.sareekart.storage;

import com.sareekart.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem storage for development/testing.
 * Files stored under app.storage.upload-dir; served by Spring's static
 * resource handler at /uploads/**.
 */
@Slf4j
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local")
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties properties;
    private final ImageValidator validator;

    @Override
    public String store(Long productId, MultipartFile file) {
        validator.validate(file);
        String ext = validator.extensionFor(file.getContentType());
        String key = "products/" + productId + "/" + UUID.randomUUID() + "." + ext;

        Path dir = Paths.get(properties.getUploadDir(), "products", String.valueOf(productId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "." + ext);
            try (var is = file.getInputStream()) {
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String url = "/uploads/" + key;
            log.info("Stored locally: {} → {}", key, url);
            return url;
        } catch (IOException e) {
            log.error("Local storage failed for product {}: {}", productId, e.getMessage());
            throw new RuntimeException("File storage failed", e);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        String relative = url.substring("/uploads/".length());
        // Prevent path traversal
        if (relative.contains("..") || relative.contains("//")) return;
        try {
            Files.deleteIfExists(Paths.get(properties.getUploadDir(), relative));
            log.info("Deleted local file: {}", relative);
        } catch (IOException e) {
            log.warn("Failed to delete local file {}: {}", relative, e.getMessage());
        }
    }

    @Override
    public String extractKey(String url) {
        if (url != null && url.startsWith("/uploads/")) {
            return url.substring("/uploads/".length());
        }
        return null;
    }
}