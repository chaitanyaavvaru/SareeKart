package com.sareekart.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /payments/create-order/{orderId}.
 * Field names match the frontend CheckoutPage contract exactly:
 *   keyId, amount (rupees), currency, razorpayOrderId.
 * Contains no secrets — the Key Secret never leaves the backend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    private Long orderId;
    private String keyId;
    private String razorpayOrderId;
    /** Authoritative total in rupees, recomputed from the stored order. */
    private Long amount;
    private String currency;
}