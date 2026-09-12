package com.sareekart.controller;

import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.User;
import com.sareekart.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administrative REST Controller for Return Claims Moderation & Reverse Logistics.
 * Restricted strictly to staff with roles OWNER, MANAGER, or ADMIN.
 */
@RestController
@RequestMapping("/api/admin/returns")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminReturnController {

    private final ReturnService returnService;

    /**
     * Retrieve filterable list of all return claims.
     * Supports status filters: ALL, PENDING, APPROVED, PICKUP_SCHEDULED, REJECTED, COMPLETED.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getAllReturns(
            @AuthenticationPrincipal User adminUser,
            @RequestParam(value = "status", required = false) String status) {
        log.info("Staff member #{} ({}) fetching returns with status filter: {}",
                adminUser != null ? adminUser.getId() : "unknown",
                adminUser != null ? adminUser.getRole() : "unknown",
                status);
        List<ReturnResponse> returns = returnService.getAllReturnsForAdmin(status);
        return ResponseEntity.ok(ApiResponse.success("Return claims fetched successfully", returns));
    }

    /**
     * Moderate status of a return claim.
     * Allows staff to approve, assign courier partner & reverse tracking AWB,
     * finalize refund/completion, or reject with a mandatory explanation note.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReturnResponse>> updateReturnStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User adminUser,
            @Valid @RequestBody ReturnStatusUpdateRequest request) {
        log.info("Staff member #{} ({}) updating return claim #{} to status: {}",
                adminUser != null ? adminUser.getId() : "unknown",
                adminUser != null ? adminUser.getRole() : "unknown",
                id, request.getStatus());
        ReturnResponse updated = returnService.updateReturnStatus(id, request, adminUser);
        return ResponseEntity.ok(ApiResponse.success("Return status updated successfully", updated));
    }
}
