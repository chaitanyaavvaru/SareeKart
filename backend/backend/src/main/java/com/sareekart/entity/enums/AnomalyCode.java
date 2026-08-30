package com.sareekart.entity.enums;

/**
 * Structured reconciliation anomaly codes (Phase 10).
 * Persisted in reconciliation_anomalies.code — never rename values.
 */
public enum AnomalyCode {
    /** Observed gateway refund exceeds captured amount or remaining refundable. */
    REFUND_AMOUNT_EXCEEDS_CAPTURE,
    /** Webhook references a payment unknown to local state. */
    PAYMENT_NOT_FOUND,
    /** Gateway captured payment for an order already expired/cancelled. */
    ORDER_ALREADY_CANCELLED_BUT_CAPTURED,
    /** Gateway does not recognise a locally-PENDING refund id. */
    UNKNOWN_REFUND,
    /** Gateway refund payload conflicts with local financial state. */
    GATEWAY_LOCAL_STATE_MISMATCH,
    /** Refund still PENDING at gateway after the retry budget is exhausted. */
    REFUND_STUCK_PENDING,
    /** Local state recovered via polling because the webhook never arrived. */
    WEBHOOK_MISSING
}