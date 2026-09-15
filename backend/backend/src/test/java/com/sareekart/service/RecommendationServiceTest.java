package com.sareekart.service;

import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private GraphService graphService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private HybridRankingService hybridRankingService;

    @Mock
    private VectorSearchService vectorSearchService;

    private RecommendationServiceImpl recommendationService;
    private final ProductMapper productMapper = new ProductMapper();

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
                graphService,
                productRepository,
                productMapper,
                hybridRankingService,
                vectorSearchService
        );
    }

    @Test
    @DisplayName("1. Hybrid Candidate Ranking: Successfully calls hybrid ranker and returns scored DTOs")
    void getFrequentlyBoughtTogether_usesHybridRanker() {
        when(graphService.findFrequentlyBoughtTogether(eq(1L), anyInt())).thenReturn(List.of(3L, 2L));

        ProductResponse pr3 = ProductResponse.builder().id(3L).name("Kanchipuram Silk").price(BigDecimal.valueOf(15000)).active(true).build();
        ProductResponse pr2 = ProductResponse.builder().id(2L).name("Banarasi Saree").price(BigDecimal.valueOf(8000)).active(true).build();

        ScoredProductResponse s3 = ScoredProductResponse.builder().product(pr3).recommendationScore(0.92).reasons(List.of("High co-purchase")).build();
        ScoredProductResponse s2 = ScoredProductResponse.builder().product(pr2).recommendationScore(0.85).reasons(List.of("Color harmony")).build();

        when(hybridRankingService.rankCandidates(anyList(), eq(1L), isNull(), isNull(), eq("FREQUENTLY_BOUGHT_TOGETHER"), eq(2)))
                .thenReturn(List.of(s3, s2));

        List<ProductResponse> results = recommendationService.getFrequentlyBoughtTogether(1L, 2);

        assertEquals(2, results.size());
        assertEquals(3L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());
    }

    @Test
    @DisplayName("2. Inactive Product Filtering: Fallback path discards inactive products")
    void getCustomersAlsoViewed_fallbackDiscardsInactive() {
        when(graphService.findCustomersAlsoViewed(eq(1L), anyInt())).thenReturn(List.of(10L, 11L));
        when(hybridRankingService.rankCandidates(anyList(), eq(1L), isNull(), isNull(), eq("CUSTOMERS_ALSO_VIEWED"), eq(1)))
                .thenThrow(new RuntimeException("Simulated ranker timeout"));

        Product activeP = Product.builder().id(10L).name("Active Saree").price(BigDecimal.valueOf(5000)).active(true).build();
        Product inactiveP = Product.builder().id(11L).name("Discontinued Saree").price(BigDecimal.valueOf(6000)).active(false).build();

        when(productRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(activeP, inactiveP));

        List<ProductResponse> results = recommendationService.getCustomersAlsoViewed(1L, 1);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getId());
        assertTrue(results.get(0).getActive());
    }

    @Test
    @DisplayName("3. Similar Sarees: Traversal returns candidates matching structural & semantic taxonomy")
    void getSimilarSarees_hydratesFromHybridOrFallback() {
        when(graphService.findSimilarSarees(eq(5L), anyInt())).thenReturn(List.of(7L, 8L, 9L));

        Product p7 = Product.builder().id(7L).name("Saree 7").price(BigDecimal.valueOf(7000)).active(true).build();
        Product p8 = Product.builder().id(8L).name("Saree 8").price(BigDecimal.valueOf(8000)).active(true).build();
        Product p9 = Product.builder().id(9L).name("Saree 9").price(BigDecimal.valueOf(9000)).active(true).build();

        ProductResponse pr7 = productMapper.toResponse(p7);
        ProductResponse pr8 = productMapper.toResponse(p8);
        ProductResponse pr9 = productMapper.toResponse(p9);

        ScoredProductResponse s7 = ScoredProductResponse.builder().product(pr7).recommendationScore(0.95).build();
        ScoredProductResponse s8 = ScoredProductResponse.builder().product(pr8).recommendationScore(0.88).build();
        ScoredProductResponse s9 = ScoredProductResponse.builder().product(pr9).recommendationScore(0.82).build();

        when(hybridRankingService.rankCandidates(anyList(), eq(5L), isNull(), isNull(), eq("SIMILAR"), eq(3)))
                .thenReturn(List.of(s7, s8, s9));

        List<ProductResponse> results = recommendationService.getSimilarSarees(5L, 3);

        assertEquals(3, results.size());
        assertEquals(7L, results.get(0).getId());
        assertEquals(8L, results.get(1).getId());
        assertEquals(9L, results.get(2).getId());
    }

    @Test
    @DisplayName("4. Personalized Recommendations: Logged-in user affinity matches")
    void getPersonalizedRecommendations_forLoggedInUser() {
        when(graphService.findPersonalizedRecommendations(eq(100L), anyInt())).thenReturn(List.of(12L, 15L));

        ProductResponse pr12 = ProductResponse.builder().id(12L).name("Weave 12").price(BigDecimal.valueOf(11000)).active(true).build();
        ProductResponse pr15 = ProductResponse.builder().id(15L).name("Weave 15").price(BigDecimal.valueOf(14000)).active(true).build();

        when(hybridRankingService.rankCandidates(anyList(), isNull(), eq(100L), isNull(), eq("PERSONALIZED"), eq(2)))
                .thenReturn(List.of(
                        ScoredProductResponse.builder().product(pr12).recommendationScore(0.91).build(),
                        ScoredProductResponse.builder().product(pr15).recommendationScore(0.87).build()
                ));

        List<ProductResponse> results = recommendationService.getPersonalizedRecommendations(100L, 2);

        assertEquals(2, results.size());
        assertEquals(12L, results.get(0).getId());
        assertEquals(15L, results.get(1).getId());
    }

    @Test
    @DisplayName("5. Personalized Recommendations for Guest: Gracefully defaults to MySQL active catalog")
    void getPersonalizedRecommendations_forGuestUser() {
        Product p = Product.builder().id(1L).name("Bestseller Saree").price(BigDecimal.valueOf(9999)).active(true).build();
        when(hybridRankingService.rankCandidates(anyList(), isNull(), isNull(), isNull(), eq("PERSONALIZED"), eq(4)))
                .thenReturn(List.of());
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(p)));

        List<ProductResponse> results = recommendationService.getPersonalizedRecommendations(null, 4);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
        verify(graphService, never()).findPersonalizedRecommendations(any(), anyInt());
    }

    @Test
    @DisplayName("6. Explainable Recommendations: Returns scored products with reasons and breakdown")
    void getExplainableRecommendations_returnsDetailedScoredProducts() {
        ProductResponse pr = ProductResponse.builder().id(5L).name("Ruby Saree").price(BigDecimal.valueOf(12000)).build();
        ScoredProductResponse scored = ScoredProductResponse.builder()
                .product(pr)
                .recommendationScore(0.89)
                .reasons(List.of("94% aesthetic similarity", "Matches preferred Silk"))
                .scoreBreakdown(Map.of("semanticScore", 0.94, "affinityScore", 0.85))
                .build();

        when(hybridRankingService.rankCandidates(anyList(), eq(5L), eq(10L), eq("sess_123"), eq("SIMILAR"), eq(4)))
                .thenReturn(List.of(scored));

        List<ScoredProductResponse> results = recommendationService.getExplainableRecommendations(
                5L, 10L, "sess_123", "SIMILAR", 4
        );

        assertEquals(1, results.size());
        assertEquals(0.89, results.get(0).getRecommendationScore());
        assertEquals(2, results.get(0).getReasons().size());
        assertEquals(0.94, results.get(0).getScoreBreakdown().get("semanticScore"));
    }
}
