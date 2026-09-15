package com.sareekart.service;

import com.sareekart.dto.response.ScoredProductResponse;

import java.util.List;

/**
 * Phase 8: Hybrid Recommendation & Multi-Factor Ranking Service Interface.
 * 
 * Scores and re-ranks candidate sarees across graph traversals, dense semantic
 * vector similarity, customer profile affinity, price intent, and inventory health.
 */
public interface HybridRankingService {

    /**
     * Ranks candidates using multi-factor scoring with color family diversity and explainability.
     * 
     * @param candidateIds Initial candidate IDs (from graph traversal, vector search, or category)
     * @param currentProductId Target product ID if on a product detail page (to be excluded)
     * @param userId Authenticated user ID (or null for guest)
     * @param sessionId Active browsing session ID
     * @param surface Recommendation surface context (e.g. FREQUENTLY_BOUGHT_TOGETHER, PERSONALIZED)
     * @param limit Maximum number of ranked recommendations to return
     * @return List of ranked and scored product responses with explainability reasons
     */
    List<ScoredProductResponse> rankCandidates(
            List<Long> candidateIds,
            Long currentProductId,
            Long userId,
            String sessionId,
            String surface,
            int limit
    );
}
