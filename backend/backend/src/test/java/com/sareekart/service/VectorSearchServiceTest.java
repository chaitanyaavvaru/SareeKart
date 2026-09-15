package com.sareekart.service;

import com.sareekart.service.impl.VectorSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VectorSearchServiceTest {

    private VectorSearchServiceImpl vectorSearchService;

    @BeforeEach
    void setUp() {
        vectorSearchService = new VectorSearchServiceImpl();
    }

    @Test
    @DisplayName("1. Cosine Similarity: Identical vectors yield similarity 1.0")
    void cosineSimilarity_identicalVectors() {
        float[] v1 = new float[]{1.0f, 0.0f, 0.5f};
        float[] v2 = new float[]{1.0f, 0.0f, 0.5f};

        double sim = vectorSearchService.cosineSimilarity(v1, v2);
        assertEquals(1.0, sim, 0.0001);
    }

    @Test
    @DisplayName("2. Cosine Similarity: Orthogonal vectors yield similarity 0.0")
    void cosineSimilarity_orthogonalVectors() {
        float[] v1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] v2 = new float[]{0.0f, 1.0f, 0.0f};

        double sim = vectorSearchService.cosineSimilarity(v1, v2);
        assertEquals(0.0, sim, 0.0001);
    }

    @Test
    @DisplayName("3. Cosine Similarity: Opposite vectors yield similarity -1.0")
    void cosineSimilarity_oppositeVectors() {
        float[] v1 = new float[]{0.5f, 0.5f};
        float[] v2 = new float[]{-0.5f, -0.5f};

        double sim = vectorSearchService.cosineSimilarity(v1, v2);
        assertEquals(-1.0, sim, 0.0001);
    }

    @Test
    @DisplayName("4. Cosine Similarity: Null, empty, or mismatched dimension vectors safely yield 0.0")
    void cosineSimilarity_edgeCases() {
        assertEquals(0.0, vectorSearchService.cosineSimilarity(null, new float[]{1.0f}));
        assertEquals(0.0, vectorSearchService.cosineSimilarity(new float[]{1.0f}, null));
        assertEquals(0.0, vectorSearchService.cosineSimilarity(new float[]{}, new float[]{}));
        assertEquals(0.0, vectorSearchService.cosineSimilarity(new float[]{1.0f}, new float[]{1.0f, 2.0f}));
        assertEquals(0.0, vectorSearchService.cosineSimilarity(new float[]{0.0f, 0.0f}, new float[]{0.0f, 0.0f}));
    }

    @Test
    @DisplayName("5. Nearest Neighbor Search: Correctly ranks products by cosine distance descending")
    void findNearestNeighbors_ranksCorrectly() {
        // Target: Silk Bridal focus
        float[] target = new float[]{1.0f, 0.8f, 0.0f};

        // Product 1: High similarity
        vectorSearchService.registerEmbedding(1L, new float[]{0.9f, 0.7f, 0.0f});
        // Product 2: Moderate similarity
        vectorSearchService.registerEmbedding(2L, new float[]{0.5f, 0.2f, 0.8f});
        // Product 3: Low / orthogonal similarity
        vectorSearchService.registerEmbedding(3L, new float[]{0.0f, 0.0f, 1.0f});

        List<Long> nearest = vectorSearchService.findNearestNeighbors(target, 2, Set.of());
        assertEquals(2, nearest.size());
        assertEquals(1L, nearest.get(0));
        assertEquals(2L, nearest.get(1));
    }

    @Test
    @DisplayName("6. Exclusions Filter: Excluded product IDs are omitted from nearest neighbor results")
    void findNearestNeighbors_respectsExclusions() {
        float[] target = new float[]{1.0f, 1.0f};
        vectorSearchService.registerEmbedding(10L, new float[]{1.0f, 1.0f});
        vectorSearchService.registerEmbedding(20L, new float[]{0.9f, 0.9f});

        List<Long> nearest = vectorSearchService.findNearestNeighbors(target, 5, Set.of(10L));
        assertEquals(1, nearest.size());
        assertEquals(20L, nearest.get(0));
    }

    @Test
    @DisplayName("7. Similar Products Lookup: Product vector similarity lookup handles self-exclusion")
    void findSimilarProducts_excludesSelf() {
        vectorSearchService.registerEmbedding(1L, new float[]{1.0f, 0.5f});
        vectorSearchService.registerEmbedding(2L, new float[]{0.9f, 0.4f});
        vectorSearchService.registerEmbedding(3L, new float[]{0.1f, 0.9f});

        List<Long> similar = vectorSearchService.findSimilarProducts(1L, 2);
        assertEquals(2, similar.size());
        assertFalse(similar.contains(1L), "Target product must not recommend itself");
        assertEquals(2L, similar.get(0));
    }

    @Test
    @DisplayName("8. Cache Management: Register, retrieve, size, and clear operations function correctly")
    void cacheOperations() {
        assertEquals(0, vectorSearchService.size());
        vectorSearchService.registerEmbedding(5L, new float[]{0.1f, 0.2f});
        assertTrue(vectorSearchService.hasEmbedding(5L));
        assertEquals(1, vectorSearchService.size());
        assertNotNull(vectorSearchService.getEmbedding(5L));

        vectorSearchService.clear();
        assertEquals(0, vectorSearchService.size());
        assertFalse(vectorSearchService.hasEmbedding(5L));
    }
}
