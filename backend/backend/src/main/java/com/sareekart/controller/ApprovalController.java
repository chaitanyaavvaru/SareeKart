package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.ApprovalRequest;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.service.ApprovalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.success(approvalService.getPendingRequests()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ApprovalRequest>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.success(approvalService.getHistory()));
    }

    @Data
    public static class RequestProposalDto {
        private String entityType;
        private String targetEntityId;
        private String action;
        private String previousValue;
        private String requestedValue;
        private String reason;
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApprovalRequest>> submitRequest(
            @RequestBody RequestProposalDto dto,
            @AuthenticationPrincipal User requester) {
        if (requester == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", null));
        }

        ApprovalRequest saved = approvalService.submitRequest(
                dto.getEntityType(),
                dto.getTargetEntityId(),
                dto.getAction(),
                dto.getPreviousValue(),
                dto.getRequestedValue(),
                dto.getReason(),
                requester
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Request submitted awaiting owner approval", saved));
    }

    @Data
    public static class ReviewActionDto {
        private String note;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<ApprovalRequest>> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewActionDto dto,
            @AuthenticationPrincipal User owner) {
        if (owner == null || (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorised to perform this action", null));
        }

        String note = dto != null && dto.getNote() != null ? dto.getNote() : "Approved by Owner";
        try {
            ApprovalRequest approved = approvalService.approveRequest(id, owner, note);
            return ResponseEntity.ok(ApiResponse.success("Request approved and committed successfully", approved));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<ApprovalRequest>> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewActionDto dto,
            @AuthenticationPrincipal User owner) {
        if (owner == null || (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorised to perform this action", null));
        }

        String note = dto != null && dto.getNote() != null ? dto.getNote() : "Rejected by Owner";
        try {
            ApprovalRequest rejected = approvalService.rejectRequest(id, owner, note);
            return ResponseEntity.ok(ApiResponse.success("Request rejected", rejected));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), null));
        }
    }
}
