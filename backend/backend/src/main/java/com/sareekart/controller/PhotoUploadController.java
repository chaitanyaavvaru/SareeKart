package com.sareekart.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Handles offline (local-network) photo uploads for saree products and inventory drapes.
 * Files are stored in the local filesystem under uploads/saree-photos/
 * and served back as a public static resource via /uploads/** mapping.
 *
 * Roles allowed: OWNER, MANAGER, ADMIN
 */
@RestController
@RequestMapping("/api/admin/photos")
@RequiredArgsConstructor
@Slf4j
public class PhotoUploadController {

    private final com.sareekart.repository.ProductRepository productRepository;
    private final com.sareekart.repository.InventoryItemRepository inventoryItemRepository;

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "image/jfif"
    );
    private static final Path UPLOAD_DIR = Paths.get("uploads", "saree-photos").toAbsolutePath();

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "sku", required = false) String sku) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No file selected"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Unsupported image format (" + contentType + "). Please upload JPG, PNG, WebP, or GIF."
            ));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "File exceeds 15 MB limit (" + (file.getSize() / (1024 * 1024)) + " MB)"
            ));
        }

        try {
            Files.createDirectories(UPLOAD_DIR);

            String ext = "jpg";
            if (contentType.contains("/")) {
                String sub = contentType.split("/")[1].toLowerCase().replace("jpeg", "jpg");
                if (sub.length() <= 5) ext = sub;
            }

            String prefix = "";
            if (productId != null) {
                prefix = "product-" + productId + "-";
            } else if (sku != null && !sku.isBlank()) {
                prefix = "sku-" + sku.replaceAll("[^a-zA-Z0-9_-]", "") + "-";
            } else {
                prefix = "product-new-";
            }

            String filename = prefix + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path dest = UPLOAD_DIR.resolve(filename);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "/uploads/saree-photos/" + filename;
            log.info("Saree photo uploaded successfully: {} (size: {} bytes)", url, file.getSize());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", url,
                    "filename", filename,
                    "size", file.getSize(),
                    "contentType", contentType
            ));
        } catch (Exception e) {
            log.error("Failed to store saree photo upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to save photo on server: " + e.getMessage()
            ));
        }
    }

    /**
     * Bulk upload – accepts up to 10 photos in one request.
     */
    @PostMapping(value = "/upload/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadBulk(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "productId", required = false) Long productId,
            @RequestParam(value = "sku", required = false) String sku) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No files provided"));
        }

        if (files.size() > 10) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Maximum 10 files per batch"));
        }

        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Could not create uploads directory"));
        }

        List<String> urls = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (file.isEmpty()) continue;

                String contentType = file.getContentType();
                if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
                    errors.add(file.getOriginalFilename() + ": unsupported format");
                    continue;
                }
                if (file.getSize() > MAX_FILE_SIZE) {
                    errors.add(file.getOriginalFilename() + ": exceeds 15 MB");
                    continue;
                }

                String ext = "jpg";
                if (contentType.contains("/")) {
                    String sub = contentType.split("/")[1].toLowerCase().replace("jpeg", "jpg");
                    if (sub.length() <= 5) ext = sub;
                }

                String prefix = "";
                if (productId != null) {
                    prefix = "product-" + productId + "-";
                } else if (sku != null && !sku.isBlank()) {
                    prefix = "sku-" + sku.replaceAll("[^a-zA-Z0-9_-]", "") + "-";
                } else {
                    prefix = "product-new-";
                }

                String filename = prefix + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
                Path dest = UPLOAD_DIR.resolve(filename);

                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                String photoUrl = "/uploads/saree-photos/" + filename;
                urls.add(photoUrl);
            } catch (Exception e) {
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", errors.isEmpty(),
                "uploaded", urls,
                "errors", errors
        ));
    }

    /**
     * Delete a previously uploaded photo by filename.
     */
    @DeleteMapping("/{filename}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePhoto(@PathVariable String filename) {
        try {
            String safe = Paths.get(filename).getFileName().toString();
            Path target = UPLOAD_DIR.resolve(safe);
            if (Files.exists(target)) {
                Files.delete(target);
                return ResponseEntity.ok(Map.of("success", true, "deleted", safe));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", "Photo not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Could not delete photo: " + e.getMessage()));
        }
    }

    @GetMapping("/by-sku/{sku}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getPhotosBySku(@PathVariable String sku) {
        return inventoryItemRepository.findBySku(sku)
                .map(item -> {
                    List<String> images = List.of();
                    if (item.getProductId() != null) {
                        images = productRepository.findById(item.getProductId())
                                .map(com.sareekart.entity.Product::getImages)
                                .orElse(List.of());
                    }
                    return ResponseEntity.ok(Map.<String, Object>of(
                            "success", true,
                            "sku", sku,
                            "productId", item.getProductId() != null ? item.getProductId() : 0,
                            "images", images
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "SKU not found")));
    }

    private void attachPhotoToProduct(String url, Long productId, String sku) {
        Long targetProductId = productId;
        if (targetProductId == null && sku != null && !sku.isBlank()) {
            inventoryItemRepository.findBySku(sku).ifPresent(item -> {
                if (item.getProductId() != null) {
                    attachToProductById(url, item.getProductId());
                }
            });
            return;
        }
        if (targetProductId != null) {
            attachToProductById(url, targetProductId);
        }
    }

    private void attachToProductById(String url, Long productId) {
        try {
            productRepository.findById(productId).ifPresent(p -> {
                if (p.getImages() == null) {
                    p.setImages(new ArrayList<>());
                }
                if (!p.getImages().contains(url)) {
                    p.getImages().add(url);
                    productRepository.save(p);
                    log.info("Attached uploaded photo {} to product #{}", url, productId);
                }
            });
        } catch (Exception e) {
            log.warn("Could not automatically link photo {} to product {}: {}", url, productId, e.getMessage());
        }
    }
}
