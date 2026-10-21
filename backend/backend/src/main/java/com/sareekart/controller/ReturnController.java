package com.sareekart.controller;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.User;
import com.sareekart.service.ReturnService;
import com.sareekart.service.storage.FileStorageService;
import com.sareekart.service.storage.StorageCategory;
import com.sareekart.service.storage.StoredFile;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

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
     * Authenticated defect photo upload using FileStorageService abstraction.
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
            StoredFile storedFile = fileStorageService.store(targetFile, StorageCategory.RETURN_PHOTOS, "return");

            log.info("Defect condition photo uploaded by user #{}: {} (size: {} bytes)",
                    user != null ? user.getId() : "anon", storedFile.getUrl(), targetFile.getSize());

            Map<String, String> data = new HashMap<>();
            data.put("url", storedFile.getUrl());
            data.put("filename", storedFile.getFilename());

            return ResponseEntity.ok(ApiResponse.success("Photo uploaded successfully", data));

        } catch (Exception e) {
            log.error("Failed to store return photo upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save photo on server: " + e.getMessage()));
        }
    }
}
