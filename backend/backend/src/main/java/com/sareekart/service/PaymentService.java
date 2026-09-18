package com.sareekart.service;

import com.sareekart.dto.request.PaymentVerificationRequest;
import com.sareekart.dto.response.OrderResponse;
import com.sareekart.dto.response.PaymentOrderResponse;

public interface PaymentService {

    PaymentOrderResponse createRazorpayOrder(Long orderId, Long userId);

    OrderResponse verifyPaymentSignature(PaymentVerificationRequest request, Long userId);

    boolean processWebhook(String payload, String signatureHeader);
}
