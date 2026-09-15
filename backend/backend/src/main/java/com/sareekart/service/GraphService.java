package com.sareekart.service;

import java.util.List;
import java.util.Map;

/**
 * Phase 7: Knowledge Graph Service interface.
 * 
 * Provides structural catalog projection, behavioral event ingestion,
 * and graph traversal queries using Neo4j.
 * 
 * Non-blocking guarantee: All implementations must handle Neo4j unavailability
 * gracefully without propagating fatal exceptions to critical commerce paths.
 */
public interface GraphService {

    /**
     * Initializes unique schema constraints in Neo4j.
     */
    void initializeSchemaConstraints();

    /**
     * Idempotently seeds categories, fabrics, occasions, colors, and products from MySQL into Neo4j.
     */
    void seedCatalog();

    /**
     * Syncs a single product's node and relationships from MySQL to Neo4j.
     */
    void syncProduct(Long productId);

    /**
     * Marks a product inactive in Neo4j (soft deletion preserving collaborative edges).
     */
    void deactivateProduct(Long productId);

    /**
     * Detach-deletes a product if hard-deleted in MySQL.
     */
    void deleteProduct(Long productId);

    /**
     * Projects historical MySQL orders and customer events into graph edges.
     */
    void seedHistoricalInteractions();

    /**
     * Ingests a PRODUCT_VIEW event into (:User)-[:VIEWED]->(:Product).
     */
    void recordProductView(Long userId, Long productId, long dwellTimeMs, String timestamp);

    /**
     * Ingests an ADD_TO_CART event into (:User)-[:CARTED]->(:Product).
     */
    void recordAddToCart(Long userId, Long productId, String timestamp);

    /**
     * Ingests an ADD_TO_WISHLIST / REMOVE_FROM_WISHLIST event.
     */
    void recordWishlist(Long userId, Long productId, boolean active, String timestamp);

    /**
     * Ingests an ORDER_COMPLETED event into (:User)-[:PURCHASED]->(:Product).
     */
    void recordPurchase(Long userId, Long productId, double amount, String timestamp);

    /**
     * Ingests category affinity signal into (:User)-[:AFFINITY_TO]->(:Category).
     */
    void recordCategoryAffinity(Long userId, Long categoryId, String timestamp);

    /**
     * Traversal query: Products frequently purchased together with the given product.
     * Returns candidate product IDs ordered by co-occurrence strength.
     */
    List<Long> findFrequentlyBoughtTogether(Long productId, int limit);

    /**
     * Traversal query: Products viewed by customers who also viewed the given product.
     * Returns candidate product IDs ordered by co-view strength.
     */
    List<Long> findCustomersAlsoViewed(Long productId, int limit);

    /**
     * Traversal query: Personalized recommendations based on user's fabric and occasion affinity.
     */
    List<Long> findPersonalizedRecommendations(Long userId, int limit);

    /**
     * Traversal query: Structurally similar sarees sharing fabric, occasion, and color family.
     */
    List<Long> findSimilarSarees(Long productId, int limit);

    /**
     * Returns true if Neo4j is online and reachable.
     */
    boolean isAvailable();

    /**
     * Returns summary statistics of nodes and relationships in Neo4j.
     */
    Map<String, Object> getGraphStatistics();
}
