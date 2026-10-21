package com.sareekart.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Production AWS S3 implementation of FileStorageService.
 * Streams objects directly to/from S3 over private AWS network fabric (S3 Gateway Endpoint).
 * Inherits Task Role credentials automatically from Fargate container environment.
 */
@Service
@ConditionalOnProperty(name = "sareekart.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3StorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;

    @Value("${sareekart.storage.s3.bucket-name}")
    private String bucketName;

    @Value("${sareekart.storage.s3.region:ap-south-1}")
    private String region;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif"
    );

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
        String s3Key = category.getDirectory() + "/" + filename;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType != null ? contentType : EXTENSION_MIME_MAP.getOrDefault(ext, "image/jpeg"))
                    .contentLength(file.getSize())
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();

            try (InputStream in = file.getInputStream()) {
                s3Client.putObject(putRequest, RequestBody.fromInputStream(in, file.getSize()));
            }

            log.info("Uploaded object to S3: s3://{}/{} (size: {} bytes)", bucketName, s3Key, file.getSize());

        } catch (IOException | S3Exception e) {
            log.error("Failed to upload object to S3 bucket {} at key {}: {}", bucketName, s3Key, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file to cloud storage: " + e.getMessage());
        }

        String accessUrl = "/uploads/" + category.getDirectory() + "/" + filename;
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
        String s3Key = category.getDirectory() + "/" + safeName;

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Deleted object from S3: s3://{}/{}", bucketName, s3Key);
            return true;
        } catch (NoSuchKeyException e) {
            log.warn("S3 object not found for deletion: s3://{}/{}", bucketName, s3Key);
            return false;
        } catch (S3Exception e) {
            log.error("Error deleting object from S3: s3://{}/{}: {}", bucketName, s3Key, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete object from S3: " + e.getMessage());
        }
    }

    @Override
    public MediaMetadata getMetadata(StorageCategory category, String filename) {
        String safeName = sanitizeFilename(filename);
        String s3Key = category.getDirectory() + "/" + safeName;

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            HeadObjectResponse headResponse = s3Client.headObject(headRequest);

            boolean isArchived = false;
            String storageClass = headResponse.storageClassAsString() != null
                    ? headResponse.storageClassAsString()
                    : "STANDARD";

            if ("GLACIER".equalsIgnoreCase(storageClass) || "DEEP_ARCHIVE".equalsIgnoreCase(storageClass)) {
                String restoreStatus = headResponse.restore();
                if (restoreStatus == null || !restoreStatus.contains("ongoing-request=\"false\"")) {
                    isArchived = true;
                }
            }

            return MediaMetadata.builder()
                    .contentType(headResponse.contentType())
                    .contentLength(headResponse.contentLength())
                    .storageClass(storageClass)
                    .eTag(headResponse.eTag())
                    .isArchived(isArchived)
                    .build();

        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found in storage: " + safeName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found in storage: " + safeName);
            }
            log.error("Error retrieving S3 object metadata for s3://{}/{}: {}", bucketName, s3Key, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to inspect media object");
        }
    }

    @Override
    public InputStream openStream(StorageCategory category, String filename) {
        String safeName = sanitizeFilename(filename);
        String s3Key = category.getDirectory() + "/" + safeName;

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            return s3Client.getObject(getRequest);

        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found in storage: " + safeName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media object not found in storage: " + safeName);
            }
            if ("InvalidObjectState".equalsIgnoreCase(e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Evidence photo is in cold archival storage and must be restored before viewing.");
            }
            log.error("Failed to stream object from S3: s3://{}/{}: {}", bucketName, s3Key, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to stream media from cloud storage");
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
