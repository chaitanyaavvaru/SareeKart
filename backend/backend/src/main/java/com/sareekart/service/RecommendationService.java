package com.sareekart.service;

import com.sareekart.dto.response.ProductResponse;

import java.util.List;

/**
 * Phase 7: Recommendation Service interface.
 * 
 * Generates recommendation candidates from Neo4j graph traversals and hydrates
 * live catalog details, prices, and stock from authoritative MySQL storage.
 * 
 * Non-blocking guarantee: Automatically falls back to deterministic MySQL
 * queries if Neo4j is offline, timed out, or returns insufficient candidates.
 */
public interface RecommendationService {

    List<ProductResponse> getFrequentlyBoughtTogether(Long productId, int limit);

    List<ProductResponse> getCustomersAlsoViewed(Long productId, int limit);

    List<ProductResponse> getPersonalizedRecommendations(Long userId, int limit);

    List<ProductResponse> getSimilarSarees(Long productId, int limit);
}
