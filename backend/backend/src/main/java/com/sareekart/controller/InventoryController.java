package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.InventoryItem;
import com.sareekart.entity.User;
import com.sareekart.repository.InventoryItemRepository;
import com.sareekart.service.ApprovalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;
    private final ApprovalService approvalService;
    private final com.sareekart.repository.ProductRepository productRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.success(inventoryItemRepository.findAllByOrderByUpdatedAtDesc()));
    }

    @Data
    public static class AdjustStockRequest {
        private String sku;
        private Integer quantityAdjustment; // positive for restock, negative for deduction
        private String reason;
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> adjustStock(
            @RequestBody AdjustStockRequest req,
            @AuthenticationPrincipal User user) {
        InventoryItem item = inventoryItemRepository.findBySku(req.getSku())
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + req.getSku()));

        int prevOnHand = item.getOnHand();
        int newOnHand = Math.max(0, prevOnHand + req.getQuantityAdjustment());

        if (user != null && (user.getRole() == com.sareekart.entity.Role.OWNER || user.getRole() == com.sareekart.entity.Role.ADMIN)) {
            item.setOnHand(newOnHand);
            item.recalculateStatus();
            inventoryItemRepository.save(item);
            if (item.getProductId() != null) {
                productRepository.findById(item.getProductId()).ifPresent(p -> {
                    p.setStockQuantity(newOnHand);
                    productRepository.save(p);
                });
            }
            return ResponseEntity.ok(ApiResponse.success("Stock updated directly to " + newOnHand, "COMMITTED"));
        }

        approvalService.submitRequest(
                "INVENTORY_STOCK",
                req.getSku(),
                req.getQuantityAdjustment() >= 0 ? "RESTOCK" : "STOCK_DEDUCTION",
                "On Hand: " + prevOnHand,
                String.valueOf(req.getQuantityAdjustment()),
                req.getReason() != null ? req.getReason() : "Manual stock adjustment",
                user
        );

        return ResponseEntity.ok(ApiResponse.success("Stock adjustment submitted. Status: Pending owner approval", "PENDING"));
    }
}
