package com.sareekart.entity.enums;

/**
 * Structured refund reason codes (Phase 10).
 * Historical rows have reason_code = NULL, read as OTHER.
 */
public enum RefundReasonCode {
    CUSTOMER_REQUEST,
    OUT_OF_STOCK,
    DUPLICATE_PAYMENT,
    ORDER_CANCELLED,
    PAYMENT_RECONCILIATION,
    ADMIN_ADJUSTMENT,
    OTHER
}