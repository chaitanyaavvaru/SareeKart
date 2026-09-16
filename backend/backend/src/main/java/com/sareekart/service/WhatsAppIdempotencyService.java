package com.sareekart.service;

/**
 * Phase 10: Ensures idempotent processing of Meta WhatsApp webhooks.
 * Prevents duplicate processing, double message deliveries, and duplicate cart mutations.
 */
public interface WhatsAppIdempotencyService {

    /**
     * Attempts to acquire an atomic execution lock for a Meta WhatsApp message ID (wam_id).
     *
     * @param wamId Unique WhatsApp message identifier
     * @return true if lock acquired (first delivery), false if duplicate delivery
     */
    boolean tryAcquireLock(String wamId);

    /**
     * Releases or clears the in-flight lock if processing failed.
     */
    void releaseLock(String wamId);

    /**
     * Checks if a message ID has already been fully processed.
     */
    boolean isProcessed(String wamId);
}
