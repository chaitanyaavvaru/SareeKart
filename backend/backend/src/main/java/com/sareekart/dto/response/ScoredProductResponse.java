package com.sareekart.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Phase 8: Scored Product Response with Explainability Diagnostic Tags.
 * 
 * Packages authoritative MySQL ProductResponse with multi-factor hybrid ranking
 * score and human-readable transparency reasons.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoredProductResponse {

    private ProductResponse product;

    /**
     * Normalized hybrid recommendation score in range [0.0, 1.0].
     */
    private double recommendationScore;

    /**
     * Human-readable explainability reasons for customer and admin transparency.
     * e.g., ["Frequently co-purchased with your viewed saree", "92% weave & drape match"]
     */
    private List<String> reasons;

    /**
     * Diagnostic sub-score breakdown across ranking dimensions.
     * (graph, semantic, affinity, priceIntent, popularity)
     */
    private Map<String, Double> scoreBreakdown;
}
