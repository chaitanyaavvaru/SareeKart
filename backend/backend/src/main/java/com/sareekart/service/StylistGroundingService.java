package com.sareekart.service;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.entity.Product;

import java.util.List;

/**
 * Phase 9: Authoritative Catalog Grounding Service.
 * Ensures that the AI Stylist prompt and responses are strictly grounded in MySQL active inventory.
 */
public interface StylistGroundingService {

    /**
     * Retrieves and ranks verified in-stock candidate products matching the discovered intent.
     *
     * @param intent Discovered stylist intent
     * @param sessionId Active guest browsing session ID
     * @param userId Authenticated user ID (if available)
     * @param limit Maximum candidate pool size (e.g. 6-10)
     * @return List of verified in-stock ScoredProductResponse candidates
     */
    List<ScoredProductResponse> retrieveGroundedCandidates(StylistIntent intent, String sessionId, Long userId, int limit);

    /**
     * Fetches verified MySQL Product entities for given product IDs (ignoring out-of-stock or inactive).
     */
    List<Product> getVerifiedActiveProducts(List<Long> productIds);
}
