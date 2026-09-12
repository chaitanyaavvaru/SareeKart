package com.sareekart.service;

import com.sareekart.entity.*;
import com.sareekart.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProductRepository productRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public ApprovalRequest submitRequest(String entityType, String targetEntityId, String action,
                                         String previousValue, String requestedValue, String reason, User requester) {
        ApprovalRequest request = ApprovalRequest.builder()
                .entityType(entityType)
                .targetEntityId(targetEntityId)
                .action(action)
                .previousValue(previousValue)
                .requestedValue(requestedValue)
                .reason(reason)
                .status(ApprovalRequest.STATUS_PENDING)
                .requestedByUserId(requester.getId())
                .requestedByEmail(requester.getEmail())
                .build();

        ApprovalRequest saved = approvalRequestRepository.save(request);

        auditLogRepository.save(AuditLog.builder()
                .action("SUBMIT_REQUEST")
                .entityType(entityType)
                .entityId(targetEntityId)
                .performedByUserId(requester.getId())
                .performedByEmail(requester.getEmail())
                .performedByRole(requester.getRole().name())
                .details("Submitted request #" + saved.getId() + " (" + action + ") awaiting owner approval")
                .build());

        return saved;
    }

    public List<ApprovalRequest> getPendingRequests() {
        return approvalRequestRepository.findByStatusOrderByCreatedAtDesc(ApprovalRequest.STATUS_PENDING);
    }

    public List<ApprovalRequest> getHistory() {
        return approvalRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public ApprovalRequest approveRequest(Long requestId, User owner, String reviewNote) {
        if (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN) {
            throw new SecurityException("Not authorised to perform this action");
        }

        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        if (!ApprovalRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Request is already " + request.getStatus() + " and cannot be re-approved");
        }

        // Apply change based on entityType
        switch (request.getEntityType()) {
            case "PRODUCT_PRICE" -> {
                Long productId = Long.valueOf(request.getTargetEntityId());
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
                BigDecimal newPrice = new BigDecimal(request.getRequestedValue().trim());
                product.setPrice(newPrice);
                productRepository.save(product);
            }
            case "INVENTORY_STOCK" -> {
                String sku = request.getTargetEntityId();
                int qtyAdjustment = Integer.parseInt(request.getRequestedValue().trim());
                InventoryItem item = inventoryItemRepository.findBySku(sku).orElse(null);
                if (item != null) {
                    item.setOnHand(Math.max(0, item.getOnHand() + qtyAdjustment));
                    item.recalculateStatus();
                    inventoryItemRepository.save(item);

                    if (item.getProductId() != null) {
                        productRepository.findById(item.getProductId()).ifPresent(p -> {
                            p.setStockQuantity(item.getAvailable());
                            productRepository.save(p);
                        });
                    }
                }
            }
            case "COUPON_DELETE" -> {
                String code = request.getTargetEntityId();
                couponRepository.findByCode(code).ifPresent(c -> {
                    c.setActive(false);
                    c.setIsDeleted(true);
                    couponRepository.save(c);
                });
            }
            case "COUPON_CREATE" -> {
                String code = request.getTargetEntityId();
                double discount = Double.parseDouble(request.getRequestedValue().trim());
                Coupon coupon = couponRepository.findByCode(code).orElse(
                        Coupon.builder().code(code).build()
                );
                coupon.setDiscountPercent(discount);
                coupon.setActive(true);
                coupon.setIsDeleted(false);
                couponRepository.save(coupon);
            }
            default -> log.info("Custom approval action applied for entity: {}", request.getEntityType());
        }

        request.setStatus(ApprovalRequest.STATUS_APPROVED);
        request.setReviewedByUserId(owner.getId());
        request.setReviewedByEmail(owner.getEmail());
        request.setReviewNote(reviewNote);
        ApprovalRequest approved = approvalRequestRepository.save(request);

        auditLogRepository.save(AuditLog.builder()
                .action("APPROVE_REQUEST")
                .entityType(request.getEntityType())
                .entityId(request.getTargetEntityId())
                .performedByUserId(owner.getId())
                .performedByEmail(owner.getEmail())
                .performedByRole(owner.getRole().name())
                .details("Approved request #" + requestId + " (" + request.getAction() + "). Note: " + reviewNote)
                .build());

        return approved;
    }

    @Transactional
    public ApprovalRequest rejectRequest(Long requestId, User owner, String reviewNote) {
        if (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN) {
            throw new SecurityException("Not authorised to perform this action");
        }

        ApprovalRequest request = approvalRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found: " + requestId));

        if (!ApprovalRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Request is already " + request.getStatus() + " and cannot be re-evaluated");
        }

        request.setStatus(ApprovalRequest.STATUS_REJECTED);
        request.setReviewedByUserId(owner.getId());
        request.setReviewedByEmail(owner.getEmail());
        request.setReviewNote(reviewNote);
        ApprovalRequest rejected = approvalRequestRepository.save(request);

        auditLogRepository.save(AuditLog.builder()
                .action("REJECT_REQUEST")
                .entityType(request.getEntityType())
                .entityId(request.getTargetEntityId())
                .performedByUserId(owner.getId())
                .performedByEmail(owner.getEmail())
                .performedByRole(owner.getRole().name())
                .details("Rejected request #" + requestId + " (" + request.getAction() + "). Reason: " + reviewNote)
                .build());

        return rejected;
    }
}
