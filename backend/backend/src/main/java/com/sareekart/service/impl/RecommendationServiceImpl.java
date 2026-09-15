package com.sareekart.service.impl;

import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.GraphService;
import com.sareekart.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 7: Recommendation Service Implementation.
 * 
 * Bridges Neo4j graph traversal with MySQL authoritative product state.
 * Never exposes raw graph internal IDs or Cypher queries.
 * Non-blocking guarantee: Fast MySQL fallback if Neo4j is offline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final GraphService graphService;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFrequentlyBoughtTogether(Long productId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        try {
            candidateIds = graphService.findFrequentlyBoughtTogether(productId, cappedLimit);
        } catch (Exception e) {
            log.warn("Error querying graph for frequently bought together (falling back to MySQL): {}", e.getMessage());
        }

        List<Product> hydrated = hydrateOrderedCandidates(candidateIds);

        // Deterministic MySQL fallback if graph returned fewer candidates than requested
        if (hydrated.size() < cappedLimit) {
            int needed = cappedLimit - hydrated.size();
            Set<Long> existingIds = hydrated.stream().map(Product::getId).collect(Collectors.toSet());
            existingIds.add(productId);

            Optional<Product> targetOpt = productRepository.findById(productId);
            Long categoryId = targetOpt.map(p -> p.getCategory() != null ? p.getCategory().getId() : null).orElse(null);

            List<Product> fallbacks = fetchCategoryFallbacks(categoryId, existingIds, needed);
            hydrated.addAll(fallbacks);
        }

        return hydrated.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getCustomersAlsoViewed(Long productId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        try {
            candidateIds = graphService.findCustomersAlsoViewed(productId, cappedLimit);
        } catch (Exception e) {
            log.warn("Error querying graph for customers also viewed (falling back to MySQL): {}", e.getMessage());
        }

        List<Product> hydrated = hydrateOrderedCandidates(candidateIds);

        if (hydrated.size() < cappedLimit) {
            int needed = cappedLimit - hydrated.size();
            Set<Long> existingIds = hydrated.stream().map(Product::getId).collect(Collectors.toSet());
            existingIds.add(productId);

            Optional<Product> targetOpt = productRepository.findById(productId);
            Long categoryId = targetOpt.map(p -> p.getCategory() != null ? p.getCategory().getId() : null).orElse(null);

            List<Product> fallbacks = fetchCategoryFallbacks(categoryId, existingIds, needed);
            hydrated.addAll(fallbacks);
        }

        return hydrated.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getPersonalizedRecommendations(Long userId, int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 12));
        List<Long> candidateIds = Collections.emptyList();

        if (userId != null) {
            try {
                candidateIds = graphService.findPersonalizedRecommendations(userId, cappedLimit);
            } catch (Exception e) {
                log.warn("Error querying graph for personalized recommendations (falling back to MySQL): {}", e.getMessage());
            }
        }

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
            candidateIds = graphService.findSimilarSarees(productId, cappedLimit);
        } catch (Exception e) {
            log.warn("Error querying graph for similar sarees (falling back to MySQL): {}", e.getMessage());
        }

        List<Product> hydrated = hydrateOrderedCandidates(candidateIds);

        if (hydrated.size() < cappedLimit) {
            int needed = cappedLimit - hydrated.size();
            Set<Long> existingIds = hydrated.stream().map(Product::getId).collect(Collectors.toSet());
            existingIds.add(productId);

            Optional<Product> targetOpt = productRepository.findById(productId);
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
