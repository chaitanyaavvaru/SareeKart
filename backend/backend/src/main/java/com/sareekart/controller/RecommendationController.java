package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ProductResponse;
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
 * Phase 7: Knowledge Graph Recommendation Controller.
 * 
 * Exposes traversal-based recommendation candidates hydrated with authoritative
 * MySQL pricing, stock, and imagery.
 * 
 * Non-blocking guarantee: Resilient fallback ensures 100% storefront uptime.
 */
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Knowledge Graph Recommendations", description = "Traversal-based product discovery and customer affinity")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final GraphService graphService;

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
    @Operation(summary = "Customer affinity recommendations based on fabric, weave, and occasion")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPersonalizedRecommendations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "8") int limit) {
        Long userId = user != null ? user.getId() : null;
        List<ProductResponse> products = recommendationService.getPersonalizedRecommendations(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/similar/{productId}")
    @Operation(summary = "Structurally similar sarees matching fabric, occasion, and color family")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarSarees(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {
        List<ProductResponse> products = recommendationService.getSimilarSarees(productId, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/status")
    @Operation(summary = "Graph service health and availability status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGraphStatus() {
        Map<String, Object> stats = graphService.getGraphStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
