package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.entity.User;
import com.sareekart.service.GraphService;
import com.sareekart.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Phase 8: AI Recommendations & Hybrid Ranking Controller.
 * 
 * Exposes multi-factor hybrid recommendations bridging Neo4j graph traversals,
 * dense semantic vector search, dynamic customer taste modeling, and authoritative
 * MySQL product hydration.
 * 
 * Non-blocking guarantee: Resilient fallback ensures 100% storefront uptime.
 */
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Recommendations & Hybrid Ranking", description = "Multi-factor recommendation engine and explainable rankings")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final GraphService graphService;

    @GetMapping({"", "/"})
    @Operation(summary = "Default recommendations showcase (delegates to trending)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getDefaultRecommendations(
            @RequestParam(defaultValue = "8") int limit) {
        return getTrendingSarees(limit);
    }

    @GetMapping("/frequently-bought-together/{productId}")
    @Operation(summary = "Co-purchased sarees (Frequently bought together)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFrequentlyBoughtTogether(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {
        List<ProductResponse> products = recommendationService.getFrequentlyBoughtTogether(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/customers-also-viewed/{productId}")
    @Operation(summary = "Collaborative co-view recommendations (Customers also viewed)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getCustomersAlsoViewed(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {
        List<ProductResponse> products = recommendationService.getCustomersAlsoViewed(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/personalized")
    @Operation(summary = "Customer affinity recommendations based on dynamic taste vector, fabric, weave, and occasion")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPersonalizedRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "8") int limit) {
        Long userId = user != null ? user.getId() : null;
        List<ProductResponse> products = recommendationService.getPersonalizedRecommendations(userId, sessionId, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/similar/{productId}")
    @Operation(summary = "Structurally & semantically similar sarees matching fabric, occasion, and dense vector embedding")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarSarees(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {
        List<ProductResponse> products = recommendationService.getSimilarSarees(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/trending")
    @Operation(summary = "Trending heritage sarees ranked by 7-day velocity and popularity")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTrendingSarees(
            @RequestParam(defaultValue = "8") int limit) {
        List<ProductResponse> products = recommendationService.getTrendingSarees(limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/complete-the-look/{productId}")
    @Operation(summary = "Complete The Look: Complementary accessories, contrast blouses, and pairing sarees")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getCompleteTheLook(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {
        List<ProductResponse> products = recommendationService.getCompleteTheLook(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/explainable/{productId}")
    @Operation(summary = "Explainable recommendations with multi-factor score and transparency tags")
    public ResponseEntity<ApiResponse<List<ScoredProductResponse>>> getExplainableRecommendations(
            @PathVariable Long productId,
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "SIMILAR") String surface,
            @RequestParam(defaultValue = "6") int limit) {
        Long userId = user != null ? user.getId() : null;
        List<ScoredProductResponse> scored = recommendationService.getExplainableRecommendations(
                productId, userId, sessionId, surface, limit
        );
        return ResponseEntity.ok(ApiResponse.success(scored));
    }

    @GetMapping("/status")
    @Operation(summary = "Graph service and recommendation engine health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGraphStatus() {
        Map<String, Object> stats = graphService.getGraphStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
