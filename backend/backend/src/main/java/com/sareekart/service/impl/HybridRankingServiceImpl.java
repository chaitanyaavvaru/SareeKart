package com.sareekart.service.impl;

import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.dto.response.customer.CustomerAffinityResponse;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.CustomerEventRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 8: Hybrid Recommendation & Multi-Factor Ranking Service Implementation.
 * 
 * Aggregates candidate pools, applies non-negotiable MySQL authoritative hard filters,
 * calculates multi-factor composite scores, enforces color family diversity, and generates
 * transparent explainability diagnostic tags.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRankingServiceImpl implements HybridRankingService {

    private final ProductRepository productRepository;
    private final VectorSearchService vectorSearchService;
    private final CustomerTasteVectorService customerTasteVectorService;
    private final CustomerBehaviorService customerBehaviorService;
    private final CustomerEventRepository customerEventRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ScoredProductResponse> rankCandidates(
            List<Long> candidateIds,
            Long currentProductId,
            Long userId,
            String sessionId,
            String surface,
            int limit
    ) {
        int boundedLimit = Math.max(1, Math.min(limit, 24));

        // 1. Prepare candidate pool
        Set<Long> candidateIdSet = new LinkedHashSet<>();
        if (candidateIds != null) {
            candidateIdSet.addAll(candidateIds);
        }

        // If candidate pool is small, supplement with semantic neighbors from vector search
        if (candidateIdSet.size() < boundedLimit) {
            if (currentProductId != null) {
                List<Long> similar = vectorSearchService.findSimilarProducts(currentProductId, boundedLimit);
                candidateIdSet.addAll(similar);
            }
        }

        // Exclude current product ID from candidate pool
        if (currentProductId != null) {
            candidateIdSet.remove(currentProductId);
        }

        // 2. Fetch Authoritative MySQL Products & Apply Hard Filters
        List<Product> rawProducts = productRepository.findAllById(candidateIdSet);
        Map<Long, Product> activeInStockMap = rawProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Supplement with active catalog if candidates still fall short
        if (activeInStockMap.size() < boundedLimit) {
            int needed = boundedLimit - activeInStockMap.size();
            var activePage = productRepository.findByActiveTrue(PageRequest.of(0, needed + activeInStockMap.size() + 2));
            if (activePage != null && activePage.getContent() != null) {
                for (Product p : activePage.getContent()) {
                    if (currentProductId != null && p.getId().equals(currentProductId)) continue;
                    if (p.getStockQuantity() != null && p.getStockQuantity() > 0) {
                        activeInStockMap.putIfAbsent(p.getId(), p);
                    }
                    if (activeInStockMap.size() >= boundedLimit) break;
                }
            }
        }

        if (activeInStockMap.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Load Customer Taste Vector & Affinity Profile
        Optional<float[]> customerTasteVector = customerTasteVectorService.computeCustomerTasteVector(userId, sessionId);
        CustomerAffinityResponse customerAffinity = null;
        if (userId != null) {
            try {
                customerAffinity = customerBehaviorService.getCustomerAffinityProfile(userId);
            } catch (Exception e) {
                log.debug("Customer affinity profile unavailable for user {}: {}", userId, e.getMessage());
            }
        }

        float[] targetProductVector = (currentProductId != null) ? vectorSearchService.getEmbedding(currentProductId) : null;

        // 4. Determine Surface Weight Presets
        SurfaceWeights weights = resolveWeights(surface);

        // 5. Score Candidates
        List<CandidateScore> scoredList = new ArrayList<>();
        List<Long> originalOrder = (candidateIds != null) ? candidateIds : Collections.emptyList();

        for (Product product : activeInStockMap.values()) {
            Long pid = product.getId();

            // S_graph: Position in original graph traversal
            double sGraph = 0.0;
            int graphRank = originalOrder.indexOf(pid);
            if (graphRank >= 0) {
                sGraph = Math.max(0.2, 1.0 - (graphRank * 0.1));
            }

            // S_semantic: Cosine similarity with customer taste or target product
            double sSemantic = 0.5;
            float[] pVector = vectorSearchService.getEmbedding(pid);
            if (pVector != null) {
                if (customerTasteVector.isPresent()) {
                    double cos = vectorSearchService.cosineSimilarity(customerTasteVector.get(), pVector);
                    sSemantic = Math.max(0.0, (1.0 + cos) / 2.0);
                } else if (targetProductVector != null) {
                    double cos = vectorSearchService.cosineSimilarity(targetProductVector, pVector);
                    sSemantic = Math.max(0.0, (1.0 + cos) / 2.0);
                }
            }

            // S_affinity: Attribute match with customer profile
            double sAffinity = calculateAffinityScore(product, customerAffinity);

            // S_intent: Price sensitivity match
            double sIntent = calculateIntentScore(product, customerAffinity);

            // S_pop: Freshness and inventory velocity
            double sPop = Math.min(1.0, 0.5 + (product.getStockQuantity() > 2 ? 0.3 : 0.1));

            // Composite Score
            double totalScore = (weights.wGraph * sGraph)
                    + (weights.wSem * sSemantic)
                    + (weights.wAff * sAffinity)
                    + (weights.wIntent * sIntent)
                    + (weights.wPop * sPop);

            totalScore = Math.max(0.0, Math.min(1.0, totalScore));

            Map<String, Double> breakdown = new LinkedHashMap<>();
            breakdown.put("graphScore", roundTwo(sGraph));
            breakdown.put("semanticScore", roundTwo(sSemantic));
            breakdown.put("affinityScore", roundTwo(sAffinity));
            breakdown.put("intentScore", roundTwo(sIntent));
            breakdown.put("popularityScore", roundTwo(sPop));

            List<String> reasons = generateReasons(sGraph, sSemantic, sAffinity, sPop, product, customerAffinity);

            scoredList.add(new CandidateScore(product, totalScore, reasons, breakdown));
        }

        // 6. Sort Candidates Descending by Total Score
        scoredList.sort(Comparator.comparingDouble(CandidateScore::score).reversed());

        // 7. Enforce Color Family Diversity Capping (max 2 per color family)
        List<CandidateScore> diversified = applyDiversityFilter(scoredList, boundedLimit);

        // 8. Transform to ScoredProductResponse
        return diversified.stream()
                .map(cs -> ScoredProductResponse.builder()
                        .product(productMapper.toResponse(cs.product()))
                        .recommendationScore(roundTwo(cs.score()))
                        .reasons(cs.reasons())
                        .scoreBreakdown(cs.breakdown())
                        .build())
                .collect(Collectors.toList());
    }

    private SurfaceWeights resolveWeights(String surface) {
        if (surface == null) {
            return new SurfaceWeights(0.30, 0.30, 0.20, 0.10, 0.10);
        }
        return switch (surface.toUpperCase()) {
            case "FREQUENTLY_BOUGHT_TOGETHER" -> new SurfaceWeights(0.50, 0.20, 0.15, 0.00, 0.15);
            case "CUSTOMERS_ALSO_VIEWED" -> new SurfaceWeights(0.35, 0.35, 0.15, 0.00, 0.15);
            case "PERSONALIZED" -> new SurfaceWeights(0.20, 0.35, 0.30, 0.15, 0.00);
            case "SIMILAR" -> new SurfaceWeights(0.30, 0.50, 0.20, 0.00, 0.00);
            default -> new SurfaceWeights(0.30, 0.30, 0.20, 0.10, 0.10);
        };
    }

    private double calculateAffinityScore(Product p, CustomerAffinityResponse affinity) {
        if (affinity == null) return 0.5;

        double match = 0.0;
        String fabric = p.getFabricEntity() != null ? p.getFabricEntity().getName() : p.getFabric();
        if (fabric != null && affinity.getPreferredFabric() != null
                && fabric.equalsIgnoreCase(affinity.getPreferredFabric())) {
            match += 0.40;
        }

        String color = p.getColorEntity() != null ? p.getColorEntity().getName() : p.getColor();
        String family = p.getColorEntity() != null ? p.getColorEntity().getFamily() : null;
        if (affinity.getPreferredColor() != null) {
            if (color != null && color.equalsIgnoreCase(affinity.getPreferredColor())) {
                match += 0.35;
            } else if (family != null && family.equalsIgnoreCase(affinity.getPreferredColor())) {
                match += 0.25;
            }
        }

        if (affinity.getPreferredWeave() != null && p.getDescription() != null
                && p.getDescription().toLowerCase().contains(affinity.getPreferredWeave().toLowerCase())) {
            match += 0.25;
        }

        return Math.min(1.0, match > 0 ? match : 0.3);
    }

    private double calculateIntentScore(Product p, CustomerAffinityResponse affinity) {
        if (affinity == null || affinity.getPriceSensitivity() == null || p.getPrice() == null) {
            return 0.5;
        }

        double price = p.getPrice().doubleValue();
        String tier = affinity.getPriceSensitivity().toUpperCase();

        return switch (tier) {
            case "BUDGET" -> price <= 5000 ? 0.9 : (price > 12000 ? 0.2 : 0.6);
            case "MID", "MID_RANGE" -> price >= 4000 && price <= 15000 ? 0.9 : 0.5;
            case "LUXURY" -> price >= 10000 ? 0.95 : (price < 4000 ? 0.3 : 0.6);
            default -> 0.5;
        };
    }

    private List<String> generateReasons(
            double sGraph,
            double sSem,
            double sAff,
            double sPop,
            Product p,
            CustomerAffinityResponse affinity
    ) {
        List<String> reasons = new ArrayList<>(3);

        if (sGraph >= 0.6) {
            reasons.add("Frequently co-purchased or co-viewed with this weave");
        }
        if (sSem >= 0.70) {
            reasons.add(String.format("%.0f%% aesthetic & drape similarity", sSem * 100));
        }
        if (affinity != null && affinity.getPreferredFabric() != null && sAff >= 0.4) {
            reasons.add("Matches your preferred " + affinity.getPreferredFabric() + " weave");
        } else if (affinity != null && affinity.getPreferredColor() != null && sAff >= 0.3) {
            reasons.add("Matches your preferred " + affinity.getPreferredColor() + " palette");
        }
        if (reasons.isEmpty() || sPop >= 0.7) {
            reasons.add("Curated handloom heritage selection");
        }

        return reasons;
    }

    private List<CandidateScore> applyDiversityFilter(List<CandidateScore> scored, int limit) {
        if (scored.size() <= limit) {
            return scored;
        }

        Map<String, Integer> colorCounts = new HashMap<>();
        List<CandidateScore> diversified = new ArrayList<>(limit);
        List<CandidateScore> overflow = new ArrayList<>();
        int maxPerFamily = Math.max(2, limit / 2);

        for (CandidateScore cs : scored) {
            String family = cs.product().getColorEntity() != null ? cs.product().getColorEntity().getFamily() : "General";
            int count = colorCounts.getOrDefault(family, 0);

            if (count < maxPerFamily && diversified.size() < limit) {
                diversified.add(cs);
                colorCounts.put(family, count + 1);
            } else {
                overflow.add(cs);
            }
        }

        // Backfill if diversity limit left open slots
        while (diversified.size() < limit && !overflow.isEmpty()) {
            diversified.add(overflow.remove(0));
        }

        return diversified;
    }

    private double roundTwo(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    private record SurfaceWeights(double wGraph, double wSem, double wAff, double wIntent, double wPop) {}

    private record CandidateScore(
            Product product,
            double score,
            List<String> reasons,
            Map<String, Double> breakdown
    ) {}
}
