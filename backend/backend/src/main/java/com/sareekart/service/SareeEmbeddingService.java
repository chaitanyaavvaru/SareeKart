package com.sareekart.service;

import com.sareekart.entity.Product;

/**
 * Phase 8: Saree Canonical Text & Dense Semantic Embedding Service Interface.
 * 
 * Defines standard 384-dimensional dense representation of saree heritage,
 * fabric, color palette, and artisanal craftsmanship.
 */
public interface SareeEmbeddingService {

    /**
     * Standard dense vector dimension (aligned with all-MiniLM-L6-v2).
     */
    int EMBEDDING_DIMENSION = 384;

    /**
     * Builds structured canonical descriptor text combining weave, fabric,
     * occasion, color family, hex, and artisan craft details.
     */
    String buildCanonicalDescriptor(Product product);

    /**
     * Generates a normalized 384-dimensional dense float vector from descriptive text.
     */
    float[] generateEmbedding(String text);

    /**
     * Seeds embeddings for all active products in the MySQL catalog, registers them
     * into VectorSearchService, and projects onto Neo4j nodes if driver is reachable.
     */
    void seedCatalogEmbeddings();

    /**
     * Generates and syncs the embedding vector for a single product.
     */
    void syncProductEmbedding(Long productId);
}
