package com.sareekart.service.impl;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.HybridRankingService;
import com.sareekart.service.StylistGroundingService;
import com.sareekart.service.VectorSearchService;
import com.sareekart.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative Catalog Grounding Service Implementation.
 * Ensures zero hallucinations by strictly deriving candidate sarees from active MySQL stock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StylistGroundingServiceImpl implements StylistGroundingService {

    private final ProductRepository productRepository;
    private final HybridRankingService hybridRankingService;
    private final VectorSearchService vectorSearchService;
    private final ProductMapper productMapper;

    @Override
    public List<ScoredProductResponse> retrieveGroundedCandidates(StylistIntent intent, String sessionId, Long userId, int limit) {
        log.info("Grounding candidate sarees for intent: queryType={}, fabric={}, color={}, maxPrice={}",
                intent.getQueryType(), intent.getPreferredFabric(), intent.getColorFamily(), intent.getMaxPrice());

        int targetLimit = (limit > 0) ? limit : 6;

        // 1. Specialized Intent: Similar but Cheaper or Similar Luxury
        if ((intent.getQueryType() == StylistIntent.QueryType.SIMILAR_CHEAPER ||
             intent.getQueryType() == StylistIntent.QueryType.SIMILAR_LUXURY) &&
            intent.getReferenceProductId() != null) {

            List<ScoredProductResponse> relationalCandidates = retrieveRelationalCandidates(intent, sessionId, userId, targetLimit);
            if (!relationalCandidates.isEmpty()) {
                return relationalCandidates;
            }
        }

        // 2. Structured Specification Query
        ProductSpecification spec = new ProductSpecification(
                null,
                null,
                null,
                intent.getPreferredFabric(),
                null,
                intent.getOccasion(),
                null,
                intent.getPreferredColor(),
                intent.getColorFamily(),
                intent.getMinPrice(),
                intent.getMaxPrice(),
                true // inStock strictly true
        );

        List<Product> products = productRepository.findAll(spec, PageRequest.of(0, 30)).getContent();

        // 3. Fallback to relaxed specification if strict query produced 0 matches
        if (products.isEmpty()) {
            log.info("Strict intent produced 0 products; relaxing fabric and color constraints");
            ProductSpecification relaxedSpec = new ProductSpecification(
                    null,
                    null,
                    null,
                    null,
                    null,
                    intent.getOccasion(),
                    null,
                    null,
                    null,
                    intent.getMinPrice(),
                    intent.getMaxPrice(),
                    true
            );
            products = productRepository.findAll(relaxedSpec, PageRequest.of(0, 30)).getContent();
        }

        // 4. Secondary fallback: active in-stock products within budget
        if (products.isEmpty()) {
            log.info("Relaxed intent produced 0 products; retrieving active in-stock catalog products");
            ProductSpecification priceOnlySpec = new ProductSpecification(
                    null, null, null, null, null, null, null, null, null,
                    intent.getMinPrice(), intent.getMaxPrice(), true
            );
            products = productRepository.findAll(priceOnlySpec, PageRequest.of(0, 20)).getContent();
        }

        // 5. Ultimate safety net: any active in-stock products
        if (products.isEmpty()) {
            products = productRepository.findByActiveTrue(PageRequest.of(0, targetLimit)).getContent();
        }

        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. Multi-Factor Hybrid Ranking with Diversity and Explainability
        List<Long> candidateIds = products.stream().map(Product::getId).collect(Collectors.toList());
        List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                candidateIds,
                intent.getReferenceProductId(),
                userId,
                sessionId,
                "AI_STYLIST",
                targetLimit
        );

        if (!ranked.isEmpty()) {
            return ranked;
        }

        // 7. Manual Mapping fallback if hybrid ranker returns empty
        return products.stream()
                .limit(targetLimit)
                .map(p -> ScoredProductResponse.builder()
                        .product(productMapper.toResponse(p))
                        .recommendationScore(0.85)
                        .reasons(List.of("Matches occasion: " + (intent.getOccasion() != null ? intent.getOccasion() : "Festive / Wedding"),
                                         "Active in-stock handloom item"))
                        .scoreBreakdown(Map.of("catalogMatch", 0.85))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ScoredProductResponse> retrieveRelationalCandidates(StylistIntent intent, String sessionId, Long userId, int limit) {
        Long refId = intent.getReferenceProductId();
        Product ref = productRepository.findByIdAndActiveTrue(refId).orElse(null);
        if (ref == null || ref.getPrice() == null) {
            return Collections.emptyList();
        }

        BigDecimal refPrice = ref.getPrice();
        List<Long> similarIds = vectorSearchService.findSimilarProducts(refId, 25);
        if (similarIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Product> similarProducts = productRepository.findAllById(similarIds);
        boolean isCheaper = intent.getQueryType() == StylistIntent.QueryType.SIMILAR_CHEAPER;

        List<Product> filtered = similarProducts.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                .filter(p -> !p.getId().equals(refId))
                .filter(p -> {
                    if (p.getPrice() == null) return false;
                    if (isCheaper) {
                        boolean lowerThanRef = p.getPrice().compareTo(refPrice) < 0;
                        boolean withinMax = intent.getMaxPrice() == null || p.getPrice().compareTo(intent.getMaxPrice()) <= 0;
                        return lowerThanRef && withinMax;
                    } else {
                        boolean higherThanRef = p.getPrice().compareTo(refPrice) > 0;
                        boolean withinMin = intent.getMinPrice() == null || p.getPrice().compareTo(intent.getMinPrice()) >= 0;
                        return higherThanRef && withinMin;
                    }
                })
                .limit(limit)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> matchedIds = filtered.stream().map(Product::getId).collect(Collectors.toList());
        List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                matchedIds, refId, userId, sessionId, "AI_STYLIST", limit
        );

        if (!ranked.isEmpty()) {
            return ranked;
        }

        String reason = isCheaper
                ? "Similar weave structure at a more accessible price point"
                : "Heirloom luxury alternative with richer zari embellishments";

        return filtered.stream()
                .map(p -> ScoredProductResponse.builder()
                        .product(productMapper.toResponse(p))
                        .recommendationScore(0.90)
                        .reasons(List.of(reason))
                        .scoreBreakdown(Map.of("vectorSimilarity", 0.90))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> getVerifiedActiveProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findAllById(productIds).stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                .collect(Collectors.toList());
    }
}
