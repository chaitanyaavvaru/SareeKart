package com.sareekart.service.impl;

import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.GraphService;
import com.sareekart.service.HybridRankingService;
import com.sareekart.service.RecommendationService;
import com.sareekart.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 8: Hybrid Recommendation Service Implementation.
 * 
 * Unifies Neo4j graph candidate generation, dense semantic vector search,
 * dynamic customer taste modeling, and authoritative MySQL product hydration.
 * 
 * Non-blocking guarantee: Multi-tier fallback to deterministic MySQL catalog
 * if graph or vector layers are unavailable or return insufficient candidates.
 */
@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final GraphService graphService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final HybridRankingService hybridRankingService;
    private final VectorSearchService vectorSearchService;

    public RecommendationServiceImpl(
            GraphService graphService,
            ProductRepository productRepository,
            ProductMapper productMapper
    ) {
        this.graphService = graphService;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.hybridRankingService = null;
        this.vectorSearchService = null;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RecommendationServiceImpl(
            GraphService graphService,
            ProductRepository productRepository,
            ProductMapper productMapper,
            HybridRankingService hybridRankingService,
            VectorSearchService vectorSearchService
    ) {
        this.graphService = graphService;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
        this.hybridRankingService = hybridRankingService;
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFrequentlyBoughtTogether(Long productId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        try {
            candidateIds = graphService.findFrequentlyBoughtTogether(productId, cappedLimit * 2);
        } catch (Exception e) {
            log.warn("Error querying graph for frequently bought together: {}", e.getMessage());
        }

        if (hybridRankingService != null) {
            try {
                List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                        candidateIds, productId, null, null, "FREQUENTLY_BOUGHT_TOGETHER", cappedLimit
                );
                if (!ranked.isEmpty()) {
                    List<ProductResponse> results = ranked.stream()
                            .map(ScoredProductResponse::getProduct)
                            .collect(Collectors.toList());
                    if (results.size() >= cappedLimit) {
                        return results;
                    }
                }
            } catch (Exception e) {
                log.warn("Hybrid ranker error in getFrequentlyBoughtTogether: {}", e.getMessage());
            }
        }

        // Fallback hydration path
        return fallbackHydrate(productId, candidateIds, cappedLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getCustomersAlsoViewed(Long productId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        try {
            candidateIds = graphService.findCustomersAlsoViewed(productId, cappedLimit * 2);
        } catch (Exception e) {
            log.warn("Error querying graph for customers also viewed: {}", e.getMessage());
        }

        if (hybridRankingService != null) {
            try {
                List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                        candidateIds, productId, null, null, "CUSTOMERS_ALSO_VIEWED", cappedLimit
                );
                if (!ranked.isEmpty()) {
                    List<ProductResponse> results = ranked.stream()
                            .map(ScoredProductResponse::getProduct)
                            .collect(Collectors.toList());
                    if (results.size() >= cappedLimit) {
                        return results;
                    }
                }
            } catch (Exception e) {
                log.warn("Hybrid ranker error in getCustomersAlsoViewed: {}", e.getMessage());
            }
        }

        return fallbackHydrate(productId, candidateIds, cappedLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPersonalizedRecommendations(Long userId, int limit) {
        return getPersonalizedRecommendations(userId, null, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPersonalizedRecommendations(Long userId, String sessionId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        if (userId != null) {
            try {
                candidateIds = graphService.findPersonalizedRecommendations(userId, cappedLimit * 2);
            } catch (Exception e) {
                log.warn("Error querying graph for personalized recommendations: {}", e.getMessage());
            }
        }

        if (hybridRankingService != null) {
            try {
                List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                        candidateIds, null, userId, sessionId, "PERSONALIZED", cappedLimit
                );
                if (!ranked.isEmpty()) {
                    return ranked.stream()
                            .map(ScoredProductResponse::getProduct)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Hybrid ranker error in getPersonalizedRecommendations: {}", e.getMessage());
            }
        }

        // Guest / cold start fallback: active products from MySQL
        List<Product> hydrated = hydrateOrderedCandidates(candidateIds);
        if (hydrated.size() < cappedLimit) {
            int needed = cappedLimit - hydrated.size();
            Set<Long> existingIds = hydrated.stream().map(Product::getId).collect(Collectors.toSet());
            var page = productRepository.findByActiveTrue(PageRequest.of(0, needed + existingIds.size()));
            if (page != null && page.getContent() != null) {
                List<Product> generalFallbacks = page.getContent()
                        .stream()
                        .filter(p -> !existingIds.contains(p.getId()))
                        .limit(needed)
                        .toList();
                hydrated.addAll(generalFallbacks);
            }
        }

        return hydrated.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarSarees(Long productId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        try {
            candidateIds = graphService.findSimilarSarees(productId, cappedLimit * 2);
        } catch (Exception e) {
            log.warn("Error querying graph for similar sarees: {}", e.getMessage());
        }

        if (hybridRankingService != null) {
            try {
                List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                        candidateIds, productId, null, null, "SIMILAR", cappedLimit
                );
                if (!ranked.isEmpty()) {
                    List<ProductResponse> results = ranked.stream()
                            .map(ScoredProductResponse::getProduct)
                            .collect(Collectors.toList());
                    if (results.size() >= cappedLimit) {
                        return results;
                    }
                }
            } catch (Exception e) {
                log.warn("Hybrid ranker error in getSimilarSarees: {}", e.getMessage());
            }
        }

        return fallbackHydrate(productId, candidateIds, cappedLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getTrendingSarees(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 16));
        if (hybridRankingService != null) {
            try {
                var activePage = productRepository.findByActiveTrue(PageRequest.of(0, cappedLimit * 2));
                List<Long> activeIds = activePage != null && activePage.getContent() != null
                        ? activePage.getContent().stream().map(Product::getId).toList()
                        : Collections.emptyList();

                List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                        activeIds, null, null, null, "TRENDING", cappedLimit
                );
                if (!ranked.isEmpty()) {
                    return ranked.stream().map(ScoredProductResponse::getProduct).collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Error ranking trending sarees: {}", e.getMessage());
            }
        }

        var page = productRepository.findByActiveTrue(PageRequest.of(0, cappedLimit));
        return page != null && page.getContent() != null
                ? page.getContent().stream().map(productMapper::toResponse).collect(Collectors.toList())
                : Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getCompleteTheLook(Long productId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 8));
        Optional<Product> targetOpt = productRepository.findById(productId);
        if (targetOpt.isEmpty()) {
            return getTrendingSarees(cappedLimit);
        }

        Product target = targetOpt.get();
        Long categoryId = target.getCategory() != null ? target.getCategory().getId() : null;

        // Fetch candidates from other categories or complementary styles
        List<Product> allActive = productRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> !p.getId().equals(productId))
                .filter(p -> categoryId == null || p.getCategory() == null || !p.getCategory().getId().equals(categoryId))
                .toList();

        List<Long> complementIds = allActive.stream().map(Product::getId).toList();

        if (hybridRankingService != null) {
            try {
                List<ScoredProductResponse> ranked = hybridRankingService.rankCandidates(
                        complementIds, productId, null, null, "COMPLETE_THE_LOOK", cappedLimit
                );
                if (!ranked.isEmpty()) {
                    return ranked.stream().map(ScoredProductResponse::getProduct).collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Error ranking complete-the-look candidates: {}", e.getMessage());
            }
        }

        return fallbackHydrate(productId, complementIds, cappedLimit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoredProductResponse> getExplainableRecommendations(
            Long productId,
            Long userId,
            String sessionId,
            String surface,
            int limit
    ) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        if (productId != null) {
            try {
                candidateIds = graphService.findSimilarSarees(productId, cappedLimit * 2);
            } catch (Exception e) {
                log.debug("Graph lookup fallback in getExplainableRecommendations: {}", e.getMessage());
            }
        } else if (userId != null) {
            try {
                candidateIds = graphService.findPersonalizedRecommendations(userId, cappedLimit * 2);
            } catch (Exception e) {
                log.debug("Graph personalized lookup fallback: {}", e.getMessage());
            }
        }

        if (hybridRankingService != null) {
            return hybridRankingService.rankCandidates(candidateIds, productId, userId, sessionId, surface, cappedLimit);
        }

        List<ProductResponse> fallback = fallbackHydrate(productId, candidateIds, cappedLimit);
        return fallback.stream()
                .map(pr -> ScoredProductResponse.builder()
                        .product(pr)
                        .recommendationScore(0.5)
                        .reasons(List.of("Curated handloom selection"))
                        .build())
                .collect(Collectors.toList());
    }

    private List<ProductResponse> fallbackHydrate(Long productId, List<Long> candidateIds, int limit) {
        List<Product> hydrated = hydrateOrderedCandidates(candidateIds);

        if (hydrated.size() < limit) {
            int needed = limit - hydrated.size();
            Set<Long> existingIds = hydrated.stream().map(Product::getId).collect(Collectors.toSet());
            if (productId != null) existingIds.add(productId);

            Optional<Product> targetOpt = productId != null ? productRepository.findById(productId) : Optional.empty();
            Long categoryId = targetOpt.map(p -> p.getCategory() != null ? p.getCategory().getId() : null).orElse(null);

            List<Product> fallbacks = fetchCategoryFallbacks(categoryId, existingIds, needed);
            hydrated.addAll(fallbacks);
        }

        return hydrated.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    private List<Product> hydrateOrderedCandidates(List<Long> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Product> products = productRepository.findAllById(candidateIds);
        Map<Long, Product> productMap = products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Product> ordered = new ArrayList<>();
        for (Long id : candidateIds) {
            Product p = productMap.get(id);
            if (p != null) {
                ordered.add(p);
            }
        }
        return ordered;
    }

    private List<Product> fetchCategoryFallbacks(Long categoryId, Set<Long> excludeIds, int limit) {
        if (limit <= 0) return Collections.emptyList();

        List<Product> results = new ArrayList<>();
        if (categoryId != null) {
            var catPage = productRepository.findByCategoryIdAndActiveTrue(categoryId, PageRequest.of(0, limit + excludeIds.size()));
            if (catPage != null && catPage.getContent() != null) {
                results = catPage.getContent()
                        .stream()
                        .filter(p -> !excludeIds.contains(p.getId()))
                        .limit(limit)
                        .collect(Collectors.toList());
            }
        }

        if (results.size() < limit) {
            int remaining = limit - results.size();
            Set<Long> allExcludes = new HashSet<>(excludeIds);
            results.forEach(p -> allExcludes.add(p.getId()));

            var activePage = productRepository.findByActiveTrue(PageRequest.of(0, remaining + allExcludes.size()));
            if (activePage != null && activePage.getContent() != null) {
                List<Product> extra = activePage.getContent()
                        .stream()
                        .filter(p -> !allExcludes.contains(p.getId()))
                        .limit(remaining)
                        .toList();
                results.addAll(extra);
            }
        }

        return results;
    }
}
