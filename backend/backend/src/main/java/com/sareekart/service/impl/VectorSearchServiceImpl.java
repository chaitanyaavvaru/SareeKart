package com.sareekart.service.impl;

import com.sareekart.service.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Phase 8: Vector Search Service Implementation.
 * 
 * Provides thread-safe, sub-millisecond in-memory cosine similarity and nearest-neighbor
 * retrieval for product dense vector representations.
 */
@Service
@Slf4j
public class VectorSearchServiceImpl implements VectorSearchService {

    private final Map<Long, float[]> embeddingCache = new ConcurrentHashMap<>();

    @Override
    public double cosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length == 0 || v2.length == 0 || v1.length != v2.length) {
            return 0.0;
        }

        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        if (norm1 <= 1e-12 || norm2 <= 1e-12) {
            return 0.0;
        }

        double similarity = dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
        // Bound numerical precision anomalies to [-1.0, 1.0]
        return Math.max(-1.0, Math.min(1.0, similarity));
    }

    @Override
    public List<Long> findNearestNeighbors(float[] targetVector, int limit, Set<Long> excludeIds) {
        if (targetVector == null || targetVector.length == 0 || limit <= 0 || embeddingCache.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> exclusions = (excludeIds != null) ? excludeIds : Collections.emptySet();

        record ScoredNeighbor(Long productId, double similarity) {}

        List<ScoredNeighbor> candidates = new ArrayList<>(embeddingCache.size());

        for (Map.Entry<Long, float[]> entry : embeddingCache.entrySet()) {
            Long pid = entry.getKey();
            if (exclusions.contains(pid)) {
                continue;
            }
            double sim = cosineSimilarity(targetVector, entry.getValue());
            candidates.add(new ScoredNeighbor(pid, sim));
        }

        return candidates.stream()
                .sorted(Comparator.comparingDouble(ScoredNeighbor::similarity).reversed())
                .limit(limit)
                .map(ScoredNeighbor::productId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findSimilarProducts(Long productId, int limit) {
        if (productId == null || limit <= 0) {
            return Collections.emptyList();
        }

        float[] target = embeddingCache.get(productId);
        if (target == null) {
            return Collections.emptyList();
        }

        return findNearestNeighbors(target, limit, Set.of(productId));
    }

    @Override
    public void registerEmbedding(Long productId, float[] embedding) {
        if (productId != null && embedding != null && embedding.length > 0) {
            embeddingCache.put(productId, embedding);
        }
    }

    @Override
    public float[] getEmbedding(Long productId) {
        if (productId == null) return null;
        return embeddingCache.get(productId);
    }

    @Override
    public Map<Long, float[]> getAllEmbeddings() {
        return Collections.unmodifiableMap(embeddingCache);
    }

    @Override
    public boolean hasEmbedding(Long productId) {
        return productId != null && embeddingCache.containsKey(productId);
    }

    @Override
    public int size() {
        return embeddingCache.size();
    }

    @Override
    public void clear() {
        embeddingCache.clear();
    }
}
