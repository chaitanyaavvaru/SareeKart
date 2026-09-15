package com.sareekart.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phase 8: Vector Search Service Interface.
 * 
 * Provides sub-millisecond in-memory dense vector indexing and cosine similarity
 * calculations over saree embeddings without external SaaS dependencies.
 */
public interface VectorSearchService {

    /**
     * Computes the cosine similarity between two normalized floating-point vectors.
     * Returns a similarity score in [-1.0, 1.0], or 0.0 for zero/invalid vectors.
     */
    double cosineSimilarity(float[] v1, float[] v2);

    /**
     * Finds the nearest product IDs to a target vector sorted by descending cosine similarity.
     */
    List<Long> findNearestNeighbors(float[] targetVector, int limit, Set<Long> excludeIds);

    /**
     * Finds structurally and semantically similar products to a target product.
     */
    List<Long> findSimilarProducts(Long productId, int limit);

    /**
     * Registers or updates a product's embedding vector in the in-memory index.
     */
    void registerEmbedding(Long productId, float[] embedding);

    /**
     * Retrieves the dense embedding vector for a product, or null if not indexed.
     */
    float[] getEmbedding(Long productId);

    /**
     * Returns an unmodifiable snapshot of all indexed product embeddings.
     */
    Map<Long, float[]> getAllEmbeddings();

    /**
     * Checks if a product embedding is registered in memory.
     */
    boolean hasEmbedding(Long productId);

    /**
     * Returns the total count of indexed product vectors.
     */
    int size();

    /**
     * Clears all cached product embeddings.
     */
    void clear();
}
