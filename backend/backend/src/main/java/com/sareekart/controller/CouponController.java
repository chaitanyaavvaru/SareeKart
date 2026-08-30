package com.sareekart.controller;

import com.sareekart.dto.common.ApiResponse;
import com.sareekart.dto.coupon.CouponPreviewResponse;
import com.sareekart.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Customer coupon surface.
 *
 * POST /coupons/preview  {code}  — quotes against the caller's LIVE cart
 *                                  subtotal (server-computed; the client never
 *                                  sends money values). Preview is advisory:
 *                                  the order API re-validates under lock.
 * GET  /coupons/validate?code=   — legacy alias kept for the existing
 *                                  checkout call shape.
 *
 * No internal ids, usage counters or admin fields are exposed here.
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Customer coupon validation")
public class CouponController {

    private final CouponService couponService;

    public record CouponCodeRequest(String code) {}

    @PostMapping("/preview")
    @Operation(summary = "Preview a coupon against your current cart (server-computed)")
    public ResponseEntity<ApiResponse<CouponPreviewResponse>> preview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CouponCodeRequest request) {
        CouponPreviewResponse preview =
            couponService.preview(userDetails.getUsername(), request.code());
        return ResponseEntity.ok(ApiResponse.success(preview));
    }

    @GetMapping("/validate")
    @Operation(summary = "Alias of /preview for legacy clients")
    public ResponseEntity<ApiResponse<CouponPreviewResponse>> validate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success(
            couponService.preview(userDetails.getUsername(), code)));
    }
}