package com.sareekart.service;

import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.dto.response.customer.CustomerAffinityResponse;
import com.sareekart.entity.Color;
import com.sareekart.entity.Fabric;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.CustomerEventRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.HybridRankingServiceImpl;
import com.sareekart.service.impl.VectorSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRankingServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerTasteVectorService customerTasteVectorService;

    @Mock
    private CustomerBehaviorService customerBehaviorService;

    @Mock
    private CustomerEventRepository customerEventRepository;

    private VectorSearchServiceImpl vectorSearchService;
    private final ProductMapper productMapper = new ProductMapper();
    private HybridRankingServiceImpl hybridRankingService;

    @BeforeEach
    void setUp() {
        vectorSearchService = new VectorSearchServiceImpl();
        hybridRankingService = new HybridRankingServiceImpl(
                productRepository,
                vectorSearchService,
                customerTasteVectorService,
                customerBehaviorService,
                customerEventRepository,
                productMapper
        );
    }

    @Test
    @DisplayName("1. Hard Filtering: Inactive and out-of-stock sarees are excluded from recommendations")
    void rankCandidates_hardFiltersInactiveAndOutOfStock() {
        Product activeInStock = Product.builder()
                .id(1L).name("Active Saree").price(BigDecimal.valueOf(5000)).active(true).stockQuantity(5).build();
        Product inactive = Product.builder()
                .id(2L).name("Inactive Saree").price(BigDecimal.valueOf(6000)).active(false).stockQuantity(5).build();
        Product outOfStock = Product.builder()
                .id(3L).name("OOS Saree").price(BigDecimal.valueOf(7000)).active(true).stockQuantity(0).build();

        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(activeInStock, inactive, outOfStock));

        List<ScoredProductResponse> results = hybridRankingService.rankCandidates(
                List.of(1L, 2L, 3L), null, null, null, "SIMILAR", 3
        );

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getProduct().getId());
    }

    @Test
    @DisplayName("2. Self Exclusion: Current product ID on PDP is strictly excluded")
    void rankCandidates_excludesCurrentProduct() {
        Product p1 = Product.builder().id(1L).name("Viewed Saree").price(BigDecimal.valueOf(8000)).active(true).stockQuantity(3).build();
        Product p2 = Product.builder().id(2L).name("Alternative Saree").price(BigDecimal.valueOf(9000)).active(true).stockQuantity(4).build();

        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(p2));

        List<ScoredProductResponse> results = hybridRankingService.rankCandidates(
                List.of(1L, 2L), 1L, null, null, "CUSTOMERS_ALSO_VIEWED", 2
        );

        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getProduct().getId());
    }

    @Test
    @DisplayName("3. Multi-Factor Scoring & Explainability: Generates composite score and transparent reasons")
    void rankCandidates_computesScoreAndReasons() {
        Color redColor = Color.builder().name("Crimson").family("Red").build();
        Fabric silkFabric = Fabric.builder().name("Pure Silk").build();

        Product p = Product.builder()
                .id(5L)
                .name("Kanchipuram Silk")
                .description("Pure silk weave with grand temple border")
                .price(BigDecimal.valueOf(12000))
                .active(true)
                .stockQuantity(10)
                .colorEntity(redColor)
                .fabricEntity(silkFabric)
                .build();

        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(p));

        CustomerAffinityResponse affinity = CustomerAffinityResponse.builder()
                .preferredFabric("Pure Silk")
                .preferredColor("Crimson")
                .priceSensitivity("MID")
                .build();
        when(customerBehaviorService.getCustomerAffinityProfile(100L)).thenReturn(affinity);

        float[] tasteVec = new float[384];
        tasteVec[0] = 1.0f;
        when(customerTasteVectorService.computeCustomerTasteVector(100L, null)).thenReturn(Optional.of(tasteVec));

        float[] pVec = new float[384];
        pVec[0] = 0.9f;
        vectorSearchService.registerEmbedding(5L, pVec);

        List<ScoredProductResponse> results = hybridRankingService.rankCandidates(
                List.of(5L), null, 100L, null, "PERSONALIZED", 1
        );

        assertEquals(1, results.size());
        ScoredProductResponse scored = results.get(0);
        assertTrue(scored.getRecommendationScore() > 0.60, "Composite score should reflect strong affinity & semantic match");
        assertNotNull(scored.getReasons());
        assertFalse(scored.getReasons().isEmpty());
        assertNotNull(scored.getScoreBreakdown());
        assertTrue(scored.getScoreBreakdown().containsKey("semanticScore"));
        assertTrue(scored.getScoreBreakdown().containsKey("affinityScore"));
    }

    @Test
    @DisplayName("4. Color Diversity Capping: Limits sarees from the same color family to prevent visual monotony")
    void rankCandidates_enforcesColorDiversity() {
        Color red = Color.builder().name("Red").family("Red").build();
        Color blue = Color.builder().name("Blue").family("Blue").build();

        Product r1 = Product.builder().id(1L).name("Red 1").active(true).stockQuantity(5).price(BigDecimal.valueOf(5000)).colorEntity(red).build();
        Product r2 = Product.builder().id(2L).name("Red 2").active(true).stockQuantity(5).price(BigDecimal.valueOf(5000)).colorEntity(red).build();
        Product r3 = Product.builder().id(3L).name("Red 3").active(true).stockQuantity(5).price(BigDecimal.valueOf(5000)).colorEntity(red).build();
        Product b1 = Product.builder().id(4L).name("Blue 1").active(true).stockQuantity(5).price(BigDecimal.valueOf(5000)).colorEntity(blue).build();

        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(r1, r2, r3, b1));

        List<ScoredProductResponse> results = hybridRankingService.rankCandidates(
                List.of(1L, 2L, 3L, 4L), null, null, null, "SIMILAR", 3
        );

        assertEquals(3, results.size());

        // Max 2 red sarees allowed when limit is 3, so Blue saree must be included
        long redCount = results.stream()
                .filter(r -> "Red".equals(r.getProduct().getColorFamily()))
                .count();
        long blueCount = results.stream()
                .filter(r -> "Blue".equals(r.getProduct().getColorFamily()))
                .count();

        assertEquals(2, redCount, "Max 2 per color family should be strictly enforced");
        assertEquals(1, blueCount, "Diverse color family should be elevated into recommendations");
    }
}
