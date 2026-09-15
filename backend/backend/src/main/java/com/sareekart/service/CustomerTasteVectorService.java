package com.sareekart.service;

import java.util.Optional;

/**
 * Phase 8: Dynamic Customer Taste Vector Service Interface.
 * 
 * Computes customer aesthetic taste centroids from Phase 6 behavioral telemetry
 * using exponential recency decay (14-day half-life).
 */
public interface CustomerTasteVectorService {

    /**
     * Computes the normalized dynamic taste vector for an authenticated user or guest session.
     * Returns Optional.empty() if customer is in cold-start with insufficient interaction history.
     */
    Optional<float[]> computeCustomerTasteVector(Long userId, String sessionId);
}
