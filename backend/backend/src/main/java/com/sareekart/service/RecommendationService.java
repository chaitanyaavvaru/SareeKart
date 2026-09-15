package com.sareekart.service;

import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;

import java.util.List;

/**
 * Phase 8: Recommendation Service Interface.
 * 
 * Bridges Neo4j graph traversals, dense vector semantic search, dynamic customer
 * taste centroids, and authoritative MySQL product hydration into explainable
 * hybrid recommendations.
 * 
 * Non-blocking guarantee: Multi-tier fallback ensures 100% storefront uptime.
 */
public interface RecommendationService {

    List<ProductResponse> getFrequentlyBoughtTogether(Long productId, int limit);

    List<ProductResponse> getCustomersAlsoViewed(Long productId, int limit);

    List<ProductResponse> getPersonalizedRecommendations(Long userId, int limit);

    List<ProductResponse> getPersonalizedRecommendations(Long userId, String sessionId, int limit);

    List<ProductResponse> getSimilarSarees(Long productId, int limit);

    List<ProductResponse> getTrendingSarees(int limit);

    List<ProductResponse> getCompleteTheLook(Long productId, int limit);

    List<ScoredProductResponse> getExplainableRecommendations(
            Long productId,
            Long userId,
            String sessionId,
            String surface,
            int limit
    );
}
