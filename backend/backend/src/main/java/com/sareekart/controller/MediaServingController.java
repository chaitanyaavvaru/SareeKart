package com.sareekart.controller;

import com.sareekart.entity.User;
import com.sareekart.entity.Role;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.service.storage.FileStorageService;
import com.sareekart.service.storage.MediaMetadata;
import com.sareekart.service.storage.StorageCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Controller providing streaming access to stored media assets across local and S3 adapters.
 * Enforces category validation, path traversal defense, fine-grained access control,
 * and category-specific cache headers.
 */
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Slf4j
public class MediaServingController {

    private final FileStorageService fileStorageService;
    private final ReturnRequestRepository returnRequestRepository;

    @GetMapping("/{category}/{filename}")
    public ResponseEntity<StreamingResponseBody> serveMedia(
            @PathVariable String category,
            @PathVariable String filename,
            @AuthenticationPrincipal User user) {

        // 1. Validate Category from Whitelist
        StorageCategory storageCategory;
        try {
            storageCategory = StorageCategory.fromPath(category);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media category: " + category);
        }

        // 2. Path Traversal Guard
        String safeFilename = sanitizeFilename(filename);

        // 3. Fine-Grained Authorization for Private Return Photos
        if (storageCategory == StorageCategory.RETURN_PHOTOS) {
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to view return defect photos");
            }

            boolean isStaff = user.getRole() == Role.OWNER || user.getRole() == Role.MANAGER || user.getRole() == Role.ADMIN;
            if (!isStaff) {
                boolean isOwner = returnRequestRepository.existsByUserIdAndImageFilename(user.getId(), safeFilename);
                if (!isOwner) {
                    log.warn("Access Denied: Customer #{} attempted to view unauthorized return photo '{}'",
                            user.getId(), safeFilename);
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view this return defect photo");
                }
            }
        }

        // 4. Retrieve Media Metadata
        MediaMetadata metadata = fileStorageService.getMetadata(storageCategory, safeFilename);

        if (metadata.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Media file is archived in cold storage and must be restored before viewing.");
        }

        // 5. Construct Response Headers with Strict Cache Policies
        String cacheControl = (storageCategory == StorageCategory.SAREE_PHOTOS)
                ? "public, max-age=86400, immutable"
                : "private, no-cache, no-store, must-revalidate";

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .contentLength(metadata.getContentLength())
                .header(HttpHeaders.CACHE_CONTROL, cacheControl);

        if (metadata.getETag() != null) {
            responseBuilder.eTag(metadata.getETag());
        }

        if (storageCategory == StorageCategory.RETURN_PHOTOS) {
            responseBuilder.header(HttpHeaders.PRAGMA, "no-cache");
            responseBuilder.header(HttpHeaders.EXPIRES, "0");
        }

        // 6. Direct Chunked Streaming (Zero-Heap Accumulation)
        return responseBuilder.body(outputStream -> {
            try (InputStream in = fileStorageService.openStream(storageCategory, safeFilename)) {
                in.transferTo(outputStream);
                outputStream.flush();
            } catch (IOException e) {
                log.debug("Media client disconnected prematurely during stream: {}/{}", category, safeFilename);
            }
        });
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename must not be empty");
        }
        String safe = Paths.get(filename).getFileName().toString();
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\") || !safe.equals(filename)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename contains illegal path traversal characters");
        }
        return safe;
    }
}
