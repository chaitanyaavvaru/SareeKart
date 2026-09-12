package com.sareekart.controller;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.User;
import com.sareekart.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * REST Controller for Customer Self-Service Returns & Exchanges.
 * Provides endpoints for claim submission, customer return history,
 * order claim lookup, and authenticated defect condition photo uploads.
 */
@RestController
@RequestMapping("/api/returns")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ReturnController {

    private final ReturnService returnService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Path UPLOAD_DIR = Paths.get("uploads", "return-photos").toAbsolutePath();

    /**
     * Submit a new return or exchange request.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReturnResponse>> createReturnRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReturnCreateRequest request) {
        log.info("Customer #{} submitting return request for Order #{}", user != null ? user.getId() : "anon", request.getOrderId());
        ReturnResponse response = returnService.createReturnRequest(request, user != null ? user.getId() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Return request submitted successfully", response));
    }

    /**
     * Fetch all return and exchange claims submitted by the authenticated customer.
     */
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getMyReturnRequests(
            @AuthenticationPrincipal User user) {
        List<ReturnResponse> returns = returnService.getMyReturnRequests(user != null ? user.getId() : null);
        return ResponseEntity.ok(ApiResponse.success("Return requests retrieved successfully", returns));
    }

    /**
     * Fetch return claim telemetry for a specific order owned by the authenticated customer.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<ReturnResponse>> getReturnByOrderId(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        ReturnResponse response = returnService.getReturnRequestByOrderId(orderId, user != null ? user.getId() : null);
        if (response != null) {
            return ResponseEntity.ok(ApiResponse.success("Return claim retrieved successfully", response));
        } else {
            return ResponseEntity.ok(ApiResponse.success("No return request found for this order", null));
        }
    }

    /**
     * Authenticated defect photo upload storing in uploads/return-photos/
     * and returning accessible URL (/uploads/return-photos/...).
     * Accepts either 'file' or 'photo' parameter for frontend compatibility.
     */
    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPhoto(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        MultipartFile targetFile = (file != null && !file.isEmpty()) ? file : photo;

        if (targetFile == null || targetFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No file selected"));
        }

        String contentType = targetFile.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Unsupported image format (" + contentType + "). Please upload JPG, PNG, or WebP."));
        }

        if (targetFile.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("File exceeds 10 MB limit (" + (targetFile.getSize() / (1024 * 1024)) + " MB)"));
        }

        try {
            Files.createDirectories(UPLOAD_DIR);

            String ext = "jpg";
            if (contentType.contains("/")) {
                String sub = contentType.split("/")[1].toLowerCase().replace("jpeg", "jpg");
                if (sub.length() <= 5) {
                    ext = sub;
                }
            }

            String filename = "return-" + UUID.randomUUID().toString().substring(0, 12) + "." + ext;
            Path dest = UPLOAD_DIR.resolve(filename);

            try (InputStream in = targetFile.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "/uploads/return-photos/" + filename;
            log.info("Defect condition photo uploaded by user #{}: {} (size: {} bytes)",
                    user != null ? user.getId() : "anon", url, targetFile.getSize());

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("filename", filename);

            return ResponseEntity.ok(ApiResponse.success("Photo uploaded successfully", data));

        } catch (Exception e) {
            log.error("Failed to store return photo upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save photo on server: " + e.getMessage()));
        }
    }
}
