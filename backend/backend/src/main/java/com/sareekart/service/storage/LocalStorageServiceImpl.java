package com.sareekart.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Local filesystem implementation of FileStorageService.
 * Retained for local development, offline mock testing, and CI pipelines.
 */
@Service
@ConditionalOnProperty(name = "sareekart.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif"
    );

    public LocalStorageServiceImpl(@Value("${sareekart.storage.local.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath();
    }

    @Override
    public StoredFile store(MultipartFile file, StorageCategory category, String customPrefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty or null file");
        }

        String contentType = file.getContentType();
        String ext = resolveExtension(file.getOriginalFilename(), contentType);

        String prefix = sanitizePrefix(customPrefix);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String filename = prefix + System.currentTimeMillis() + "-" + uniqueId + "." + ext;

        Path targetDir = rootLocation.resolve(category.getDirectory()).normalize();
        Path destination = targetDir.resolve(filename).normalize();

        // Enforce path traversal guard
        if (!destination.startsWith(targetDir)) {
            throw new SecurityException("Illegal path traversal detected in file destination");
        }

        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store file locally under {}: {}", category.getDirectory(), e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save file on disk: " + e.getMessage());
        }

        String accessUrl = "/uploads/" + category.getDirectory() + "/" + filename;
        log.info("Stored local file: {} ({} bytes)", accessUrl, file.getSize());

        return StoredFile.builder()
                .url(accessUrl)
                .filename(filename)
                .size(file.getSize())
                .contentType(contentType != null ? contentType : EXTENSION_MIME_MAP.getOrDefault(ext, "image/jpeg"))
                .category(category.getDirectory())
                .build();
    }

    @Override
    public boolean delete(StorageCategory category, String filename) {
        String safeName = sanitizeFilename(filename);
        Path target = rootLocation.resolve(category.getDirectory()).resolve(safeName).normalize();

        if (!target.startsWith(rootLocation.resolve(category.getDirectory()).normalize())) {
            throw new SecurityException("Illegal path traversal sequence in delete target");
        }

        try {
            if (Files.exists(target)) {
                Files.delete(target);
                log.info("Deleted local file: {}/{}", category.getDirectory(), safeName);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to delete local file {}/{}: {}", category.getDirectory(), safeName, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete file: " + e.getMessage());
        }
    }

    @Override
    public MediaMetadata getMetadata(StorageCategory category, String filename) {
        String safeName = sanitizeFilename(filename);
        Path target = rootLocation.resolve(category.getDirectory()).resolve(safeName).normalize();

        if (!Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media file not found: " + safeName);
        }

        try {
            long size = Files.size(target);
            String ext = safeName.substring(safeName.lastIndexOf('.') + 1).toLowerCase();
            String contentType = EXTENSION_MIME_MAP.getOrDefault(ext, "application/octet-stream");

            return MediaMetadata.builder()
                    .contentType(contentType)
                    .contentLength(size)
                    .storageClass("LOCAL_FS")
                    .eTag("\"" + Files.getLastModifiedTime(target).toMillis() + "\"")
                    .isArchived(false)
                    .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file metadata");
        }
    }

    @Override
    public InputStream openStream(StorageCategory category, String filename) {
        String safeName = sanitizeFilename(filename);
        Path target = rootLocation.resolve(category.getDirectory()).resolve(safeName).normalize();

        if (!Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media file not found: " + safeName);
        }

        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to open stream for file: " + safeName);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be empty");
        }
        String safe = Paths.get(filename).getFileName().toString();
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || !safe.equals(filename)) {
            throw new SecurityException("Filename contains illegal path traversal sequences");
        }
        return safe;
    }

    private String sanitizePrefix(String customPrefix) {
        if (customPrefix == null || customPrefix.isBlank()) {
            return "file-";
        }
        return customPrefix.replaceAll("[^a-zA-Z0-9_-]", "") + "-";
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String ext = "jpg";
        if (contentType != null && contentType.contains("/")) {
            String sub = contentType.split("/")[1].toLowerCase().replace("jpeg", "jpg");
            if (ALLOWED_EXTENSIONS.contains(sub)) {
                return sub;
            }
        }
        if (originalFilename != null && originalFilename.contains(".")) {
            String candidate = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (ALLOWED_EXTENSIONS.contains(candidate)) {
                return candidate.equals("jpeg") ? "jpg" : candidate;
            }
        }
        return ext;
    }
}
