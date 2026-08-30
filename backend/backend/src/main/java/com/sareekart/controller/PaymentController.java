package com.sareekart.controller;

import com.sareekart.dto.common.ApiResponse;
import com.sareekart.dto.order.OrderResponse;
import com.sareekart.dto.payment.PaymentOrderResponse;
import com.sareekart.dto.payment.VerifyPaymentRequest;
import com.sareekart.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Payment surface.
 *
 *  POST /payments/create-order/{orderId} — CUSTOMER, owns the order
 *  POST /payments/verify                 — CUSTOMER, owns the order
 *  POST /payments/webhook                — PUBLIC, HMAC-verified via raw body
 *
 * Secrets are never present in any response; webhook auth is signature-only.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Razorpay payment APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order/{orderId}")
    @Operation(summary = "Create (or reuse) a Razorpay order for a SareeKart order")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {
        PaymentOrderResponse response =
            paymentService.createPaymentOrder(userDetails.getUsername(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay checkout signature and mark the order paid")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyPaymentRequest request) {
        OrderResponse order = paymentService.verifyPayment(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified", order));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay webhook receiver (HMAC-SHA256 over raw body)")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        paymentService.handleWebhook(rawBody, signature, eventId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("received", true)));
    }
}