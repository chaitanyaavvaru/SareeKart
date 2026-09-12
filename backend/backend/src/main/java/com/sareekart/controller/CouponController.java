package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.entity.Coupon;
import com.sareekart.entity.Role;
import com.sareekart.entity.User;
import com.sareekart.repository.CouponRepository;
import com.sareekart.service.ApprovalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CouponController {

    private final CouponRepository couponRepository;
    private final ApprovalService approvalService;

    /**
     * Validate a coupon code and return its details if valid.
     */
    @GetMapping("/coupons/validate")
    public ResponseEntity<ApiResponse<Coupon>> validateCoupon(@RequestParam String code) {
        return couponRepository.findByCodeAndIsDeletedFalse(code.trim().toUpperCase())
                .filter(Coupon::getActive)
                .filter(c -> c.getExpiryDate() == null || c.getExpiryDate().isAfter(LocalDateTime.now()))
                .map(coupon -> ResponseEntity.ok(ApiResponse.success("Coupon verified successfully", coupon)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Invalid or expired coupon code", null)));
    }

    /**
     * List all coupons in the system (Owner, Manager, Admin).
     */
    @GetMapping("/admin/coupons")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Coupon>>> getAllCoupons() {
        List<Coupon> coupons = couponRepository.findByIsDeletedFalse();
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    /**
     * Create a new coupon.
     */
    @PostMapping("/admin/coupons")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Coupon>> createCoupon(
            @RequestBody Coupon coupon,
            @AuthenticationPrincipal User user) {
        coupon.setCode(coupon.getCode().trim().toUpperCase());
        if (coupon.getActive() == null) coupon.setActive(true);
        if (coupon.getIsDeleted() == null) coupon.setIsDeleted(false);

        Coupon saved = couponRepository.save(coupon);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created successfully", saved));
    }

    @Data
    public static class DeleteCouponRequest {
        private String confirmationCode;
        private String reason;
    }

    /**
     * Remove / deactivate coupon requiring exact code confirmation string.
     */
    @PostMapping("/admin/coupons/{id}/remove")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> removeCoupon(
            @PathVariable Long id,
            @RequestBody DeleteCouponRequest req,
            @AuthenticationPrincipal User user) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));

        if (req.getConfirmationCode() == null || !req.getConfirmationCode().trim().equalsIgnoreCase(coupon.getCode())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Confirmation code does not match exact coupon code '" + coupon.getCode() + "'", null));
        }

        if (user.getRole() == Role.OWNER || user.getRole() == Role.ADMIN) {
            coupon.setActive(false);
            coupon.setIsDeleted(true);
            couponRepository.save(coupon);
            return ResponseEntity.ok(ApiResponse.success("Coupon deactivated and retained for audit records", "DEACTIVATED"));
        } else {
            // Manager submits request for owner approval
            approvalService.submitRequest(
                    "COUPON_DELETE",
                    coupon.getCode(),
                    "COUPON_DEACTIVATE",
                    "Active: " + coupon.getActive(),
                    "false",
                    req.getReason() != null ? req.getReason() : "Manager requested deactivation",
                    user
            );
            return ResponseEntity.ok(ApiResponse.success("Coupon deactivation submitted. Status: Pending owner approval", "PENDING"));
        }
    }
}
