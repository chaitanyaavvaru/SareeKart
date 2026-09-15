package com.sareekart.service;

import com.sareekart.entity.Category;
import com.sareekart.entity.Color;
import com.sareekart.entity.Fabric;
import com.sareekart.entity.Occasion;
import com.sareekart.entity.Product;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.SareeEmbeddingServiceImpl;
import com.sareekart.service.impl.VectorSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SareeEmbeddingServiceTest {

    @Mock
    private ProductRepository productRepository;

    private VectorSearchServiceImpl vectorSearchService;
    private SareeEmbeddingServiceImpl sareeEmbeddingService;

    @BeforeEach
    void setUp() {
        vectorSearchService = new VectorSearchServiceImpl();
        sareeEmbeddingService = new SareeEmbeddingServiceImpl(productRepository, vectorSearchService, null);
    }

    @Test
    @DisplayName("1. Canonical Descriptor: Generates standardized structured descriptor")
    void buildCanonicalDescriptor_formatsCorrectly() {
        Category category = Category.builder().name("Kanchipuram Silk").build();
        Fabric fabric = Fabric.builder().name("Pure Mulberry Silk").build();
        Color color = Color.builder().name("Crimson Red").family("Red").hexCode("#DC143C").build();
        Occasion occasion = Occasion.builder().name("Bridal & Wedding").build();

        Product product = Product.builder()
                .id(1L)
                .name("Grand Bridal Kanchipuram Saree")
                .description("Handwoven pure gold zari border with traditional temple motifs.")
                .price(BigDecimal.valueOf(25000))
                .category(category)
                .fabricEntity(fabric)
                .colorEntity(color)
                .occasionEntity(occasion)
                .build();

        String descriptor = sareeEmbeddingService.buildCanonicalDescriptor(product);

        assertTrue(descriptor.contains("[HERITAGE WEAVE] Kanchipuram Silk"));
        assertTrue(descriptor.contains("Pure Mulberry Silk"));
        assertTrue(descriptor.contains("[COLOR & AESTHETICS] Crimson Red (Red palette), Hex #DC143C"));
        assertTrue(descriptor.contains("[OCCASION & DRAPE] Tailored for Bridal & Wedding"));
        assertTrue(descriptor.contains("Handwoven pure gold zari border"));
    }

    @Test
    @DisplayName("2. Vector Dimension & L2 Normalization: Generated vector has 384 dims with unit norm")
    void generateEmbedding_hasCorrectDimensionsAndUnitNorm() {
        String sampleText = "[HERITAGE WEAVE] Banarasi Katan Silk. [COLOR] Ruby Red. [CRAFT] Pure zari brocade.";
        float[] vector = sareeEmbeddingService.generateEmbedding(sampleText);

        assertEquals(384, vector.length);

        double normSq = 0.0;
        for (float v : vector) {
            normSq += v * v;
        }
        assertEquals(1.0, Math.sqrt(normSq), 0.001, "Dense embedding vector must be L2-normalized");
    }

    @Test
    @DisplayName("3. Semantic Affinity: Similar bridal silks have higher similarity than casual cotton")
    void generateEmbedding_semanticClustersAlign() {
        String bridalKanchi = "[HERITAGE WEAVE] Kanchipuram Silk in Pure Silk. [OCCASION] Bridal Wedding. [COLOR] Red. [CRAFT] Gold zari temple border.";
        String bridalBanarasi = "[HERITAGE WEAVE] Banarasi in Pure Silk. [OCCASION] Bridal Wedding. [COLOR] Crimson Red. [CRAFT] Pure gold zari brocade.";
        String casualCotton = "[HERITAGE WEAVE] Cotton in Pure Cotton. [OCCASION] Casual Office. [COLOR] Blue. [CRAFT] Simple thread border.";

        float[] v1 = sareeEmbeddingService.generateEmbedding(bridalKanchi);
        float[] v2 = sareeEmbeddingService.generateEmbedding(bridalBanarasi);
        float[] v3 = sareeEmbeddingService.generateEmbedding(casualCotton);

        double simSilkBridal = vectorSearchService.cosineSimilarity(v1, v2);
        double simSilkCotton = vectorSearchService.cosineSimilarity(v1, v3);

        assertTrue(simSilkBridal > simSilkCotton,
                "Bridal silks should have higher semantic similarity than bridal silk vs casual cotton: "
                        + simSilkBridal + " vs " + simSilkCotton);
        assertTrue(simSilkBridal > 0.65, "Similar bridal weaves should score >= 0.65");
    }

    @Test
    @DisplayName("4. Empty / Null Text Resilience: Safely returns 384 zero vector without exception")
    void generateEmbedding_handlesNullAndEmpty() {
        float[] nullVec = sareeEmbeddingService.generateEmbedding(null);
        assertNotNull(nullVec);
        assertEquals(384, nullVec.length);

        float[] emptyVec = sareeEmbeddingService.generateEmbedding("   ");
        assertNotNull(emptyVec);
        assertEquals(384, emptyVec.length);
    }

    @Test
    @DisplayName("5. Catalog Seeding: Indexes all catalog products into vector search service")
    void seedCatalogEmbeddings_indexesAllProducts() {
        Product p1 = Product.builder().id(1L).name("Saree 1").active(true).build();
        Product p2 = Product.builder().id(2L).name("Saree 2").active(true).build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        sareeEmbeddingService.seedCatalogEmbeddings();

        assertEquals(2, vectorSearchService.size());
        assertTrue(vectorSearchService.hasEmbedding(1L));
        assertTrue(vectorSearchService.hasEmbedding(2L));
    }
}
