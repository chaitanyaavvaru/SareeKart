package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.InventoryItem;
import com.sareekart.entity.Role;
import com.sareekart.entity.StockTransfer;
import com.sareekart.entity.User;
import com.sareekart.repository.InventoryItemRepository;
import com.sareekart.repository.StockTransferRepository;
import com.sareekart.service.NotificationEventService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inventory")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class StockTransferController {

    private final StockTransferRepository stockTransferRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final NotificationEventService notificationEventService;

    @GetMapping("/transfers")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StockTransfer>>> getTransfers(
            @RequestParam(required = false) String status) {
        List<StockTransfer> transfers = (status != null && !status.isEmpty())
                ? stockTransferRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase())
                : stockTransferRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponse.success(transfers));
    }

    @Data
    public static class TransferRequestDto {
        private String sku;
        private String sourceWarehouse; // e.g. WH-01 Bengaluru Central
        private String targetWarehouse; // e.g. WH-02 Mumbai West
        private Integer quantity;
        private String reason;
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<StockTransfer>> requestTransfer(
            @RequestBody TransferRequestDto req,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required", null));
        }

        if (req.getSku() == null || req.getSku().trim().isEmpty() ||
            req.getSourceWarehouse() == null || req.getTargetWarehouse() == null ||
            req.getQuantity() == null || req.getQuantity() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid transfer request. SKU, source, target, and positive quantity required.", null));
        }

        if (req.getSourceWarehouse().equalsIgnoreCase(req.getTargetWarehouse())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Source and target warehouse must be distinct.", null));
        }

        String productName = "Silk Drape (" + req.getSku() + ")";
        inventoryItemRepository.findBySku(req.getSku()).ifPresent(item -> {
            if (item.getProductName() != null) {
                // Keep product name accurate
            }
        });

        InventoryItem inv = inventoryItemRepository.findBySku(req.getSku()).orElse(null);
        if (inv != null && inv.getProductName() != null) {
            productName = inv.getProductName();
        }

        boolean isOwner = user.getRole() == Role.OWNER || user.getRole() == Role.ADMIN;
        String initialStatus = isOwner ? StockTransfer.STATUS_APPROVED : StockTransfer.STATUS_PENDING;

        StockTransfer transfer = StockTransfer.builder()
                .sku(req.getSku())
                .productName(productName)
                .sourceWarehouse(req.getSourceWarehouse())
                .targetWarehouse(req.getTargetWarehouse())
                .quantity(req.getQuantity())
                .reason(req.getReason() != null ? req.getReason() : "Regional hub stock rebalance")
                .status(initialStatus)
                .requestedByUserId(user.getId())
                .requestedByEmail(user.getEmail())
                .approvedByUserId(isOwner ? user.getId() : null)
                .approvedByEmail(isOwner ? user.getEmail() : null)
                .reviewNote(isOwner ? "Directly executed by " + user.getRole() : null)
                .build();

        StockTransfer saved = stockTransferRepository.save(transfer);

        if (isOwner) {
            inventoryItemRepository.findBySku(transfer.getSku()).ifPresent(item -> {
                item.setWarehouseName(transfer.getTargetWarehouse());
                inventoryItemRepository.save(item);
            });
            log.info("Stock transfer #{} directly approved and executed by {} for SKU {}", saved.getId(), user.getEmail(), req.getSku());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Stock transfer approved and committed directly.", saved));
        } else {
            // Notify Owners of pending transfer
            notificationEventService.notifyStockTransferRequested(saved);
            log.info("Stock transfer request #{} created by {} for SKU {}", saved.getId(), user.getEmail(), req.getSku());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Transfer request submitted. Status: Pending owner approval", saved));
        }
    }

    @Data
    public static class ActionDto {
        private String note;
    }

    @PutMapping("/transfer/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<StockTransfer>> approveTransfer(
            @PathVariable Long id,
            @RequestBody(required = false) ActionDto actionDto,
            @AuthenticationPrincipal User user) {
        if (user == null || (user.getRole() != Role.OWNER && user.getRole() != Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorised to perform this action", null));
        }

        StockTransfer transfer = stockTransferRepository.findById(id).orElse(null);
        if (transfer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Stock transfer request not found: " + id, null));
        }

        if (!StockTransfer.STATUS_PENDING.equalsIgnoreCase(transfer.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Transfer #" + id + " is already " + transfer.getStatus(), null));
        }

        transfer.setStatus(StockTransfer.STATUS_APPROVED);
        transfer.setApprovedByUserId(user.getId());
        transfer.setApprovedByEmail(user.getEmail());
        transfer.setReviewNote(actionDto != null && actionDto.getNote() != null ? actionDto.getNote() : "Approved by Owner");

        StockTransfer updated = stockTransferRepository.save(transfer);

        // Update inventory item metadata if applicable
        inventoryItemRepository.findBySku(transfer.getSku()).ifPresent(item -> {
            item.setWarehouseName(transfer.getTargetWarehouse());
            inventoryItemRepository.save(item);
        });

        log.info("Stock transfer #{} approved by {}", id, user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Stock transfer #" + id + " approved and committed.", updated));
    }

    @PutMapping("/transfer/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<StockTransfer>> rejectTransfer(
            @PathVariable Long id,
            @RequestBody(required = false) ActionDto actionDto,
            @AuthenticationPrincipal User user) {
        if (user == null || (user.getRole() != Role.OWNER && user.getRole() != Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorised to perform this action", null));
        }

        StockTransfer transfer = stockTransferRepository.findById(id).orElse(null);
        if (transfer == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Stock transfer request not found: " + id, null));
        }

        if (!StockTransfer.STATUS_PENDING.equalsIgnoreCase(transfer.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Transfer #" + id + " is already " + transfer.getStatus(), null));
        }

        transfer.setStatus(StockTransfer.STATUS_REJECTED);
        transfer.setApprovedByUserId(user.getId());
        transfer.setApprovedByEmail(user.getEmail());
        transfer.setReviewNote(actionDto != null && actionDto.getNote() != null ? actionDto.getNote() : "Rejected by Owner");

        StockTransfer updated = stockTransferRepository.save(transfer);
        log.info("Stock transfer #{} rejected by {}", id, user.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Stock transfer #" + id + " rejected.", updated));
    }
}
