package com.sareekart.controller;

import com.sareekart.dto.request.PaymentVerificationRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.PaymentOrderResponse;
import com.sareekart.entity.User;
import com.sareekart.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createPaymentOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        PaymentOrderResponse response = paymentService.createRazorpayOrder(orderId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment transaction order created successfully", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OrderResponse>> verifyPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PaymentVerificationRequest request) {
        OrderResponse response = paymentService.verifyPaymentSignature(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment verified and order confirmed successfully", response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String payload) {
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Razorpay webhook processed successfully", "OK"));
    }
}
