package com.sareekart.entity.enums;

/**
 * Status of an individual refund row (NOT the payment or order).
 *
 * PENDING  → gateway call dispatched / awaiting webhook confirmation
 * SUCCESS  → gateway confirmed the money moved back to the customer
 * FAILED   → gateway rejected or errored; error_message carries detail
 */
public enum RefundStatus {
    PENDING,
    SUCCESS,
    FAILED
}