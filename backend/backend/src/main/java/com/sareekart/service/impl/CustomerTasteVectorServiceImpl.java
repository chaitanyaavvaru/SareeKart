package com.sareekart.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.entity.CustomerEvent;
import com.sareekart.repository.CustomerEventRepository;
import com.sareekart.service.CustomerTasteVectorService;
import com.sareekart.service.SareeEmbeddingService;
import com.sareekart.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Phase 8: Dynamic Customer Taste Vector Service Implementation.
 * 
 * Computes an aggregated, recency-decayed centroid across interacted saree embeddings.
 * Applies signal weighting from Phase 6 telemetry with 14-day exponential half-life.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerTasteVectorServiceImpl implements CustomerTasteVectorService {

    private final CustomerEventRepository customerEventRepository;
    private final VectorSearchService vectorSearchService;
    private final SareeEmbeddingService sareeEmbeddingService;
    private final ObjectMapper objectMapper;

    // Decay parameter for 14-day half-life: lambda = ln(2) / 14
    private static final double HALF_LIFE_DAYS = 14.0;
    private static final double LAMBDA = Math.log(2.0) / HALF_LIFE_DAYS;

    @Override
    @Transactional(readOnly = true)
    public Optional<float[]> computeCustomerTasteVector(Long userId, String sessionId) {
        List<CustomerEvent> events;

        if (userId != null) {
            events = customerEventRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100));
        } else if (sessionId != null && !sessionId.isBlank()) {
            events = customerEventRepository.findBySessionIdOrderByCreatedAtDesc(sessionId.trim(), PageRequest.of(0, 50));
        } else {
            return Optional.empty();
        }

        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }

        float[] centroid = new float[SareeEmbeddingService.EMBEDDING_DIMENSION];
        double totalPositiveWeight = 0.0;
        LocalDateTime now = LocalDateTime.now();

        for (CustomerEvent event : events) {
            Long productId = event.getEntityId();
            if (productId == null) continue;

            float[] productVec = vectorSearchService.getEmbedding(productId);
            if (productVec == null) {
                // Attempt lazy load for the product
                sareeEmbeddingService.syncProductEmbedding(productId);
                productVec = vectorSearchService.getEmbedding(productId);
            }

            if (productVec == null) {
                continue;
            }

            double baseWeight = determineEventWeight(event);
            if (Math.abs(baseWeight) < 1e-6) {
                continue;
            }

            long ageDays = 0;
            if (event.getCreatedAt() != null) {
                ageDays = Math.max(0, ChronoUnit.DAYS.between(event.getCreatedAt(), now));
            }

            double decay = Math.exp(-LAMBDA * ageDays);
            double effectiveWeight = baseWeight * decay;

            for (int i = 0; i < centroid.length; i++) {
                centroid[i] += (float) (effectiveWeight * productVec[i]);
            }

            if (effectiveWeight > 0) {
                totalPositiveWeight += effectiveWeight;
            }
        }

        if (totalPositiveWeight <= 0.01) {
            return Optional.empty();
        }

        // L2 Normalization of Customer Centroid
        double normSq = 0.0;
        for (float v : centroid) {
            normSq += v * v;
        }
        double norm = Math.sqrt(normSq);
        if (norm < 1e-9) {
            return Optional.empty();
        }

        for (int i = 0; i < centroid.length; i++) {
            centroid[i] = (float) (centroid[i] / norm);
        }

        return Optional.of(centroid);
    }

    private double determineEventWeight(CustomerEvent event) {
        String type = event.getEventType();
        if (type == null) return 0.0;

        return switch (type.toUpperCase()) {
            case "ORDER_COMPLETED" -> 5.0;
            case "CHECKOUT_INITIATED" -> 3.5;
            case "ADD_TO_CART" -> 3.0;
            case "ADD_TO_WISHLIST" -> 2.5;
            case "PRODUCT_VIEW" -> {
                long dwell = extractDwellTime(event.getMetadata());
                yield dwell >= 30000 ? 1.5 : 0.5;
            }
            case "REMOVE_FROM_CART" -> -1.5;
            case "REMOVE_FROM_WISHLIST" -> -1.0;
            default -> 0.0;
        };
    }

    private long extractDwellTime(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return 0L;
        }
        try {
            Map<String, Object> meta = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
            Object dwell = meta.get("dwellTimeMs");
            if (dwell instanceof Number n) {
                return n.longValue();
            }
        } catch (Exception ignored) {
        }
        return 0L;
    }
}
