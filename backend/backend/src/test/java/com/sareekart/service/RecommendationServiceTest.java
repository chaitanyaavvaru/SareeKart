package com.sareekart.service;

import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Category;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private GraphService graphService;

    @Mock
    private ProductRepository productRepository;

    private RecommendationServiceImpl recommendationService;
    private final ProductMapper productMapper = new ProductMapper();

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationServiceImpl(
                graphService,
                productRepository,
                productMapper
        );
    }

    @Test
    @DisplayName("1. Graph Candidate Hydration: Successfully orders and hydrates candidates from MySQL")
    void getFrequentlyBoughtTogether_hydratesOrderedCandidates() {
        when(graphService.findFrequentlyBoughtTogether(eq(1L), anyInt())).thenReturn(List.of(3L, 2L));

        Product p2 = Product.builder().id(2L).name("Banarasi Saree").price(BigDecimal.valueOf(8000)).active(true).build();
        Product p3 = Product.builder().id(3L).name("Kanchipuram Silk").price(BigDecimal.valueOf(15000)).active(true).build();

        when(productRepository.findAllById(List.of(3L, 2L))).thenReturn(List.of(p2, p3));

        List<ProductResponse> results = recommendationService.getFrequentlyBoughtTogether(1L, 2);

        assertEquals(2, results.size());
        assertEquals(3L, results.get(0).getId(), "Must preserve candidate ranking order from graph traversal");
        assertEquals(2L, results.get(1).getId());
    }

    @Test
    @DisplayName("2. Inactive Product Filtering: Inactive sarees from graph traversal are discarded")
    void getCustomersAlsoViewed_discardsInactiveProducts() {
        when(graphService.findCustomersAlsoViewed(eq(1L), anyInt())).thenReturn(List.of(10L, 11L));

        Product activeP = Product.builder().id(10L).name("Active Saree").price(BigDecimal.valueOf(5000)).active(true).build();
        Product inactiveP = Product.builder().id(11L).name("Discontinued Saree").price(BigDecimal.valueOf(6000)).active(false).build();

        when(productRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(activeP, inactiveP));

        List<ProductResponse> results = recommendationService.getCustomersAlsoViewed(1L, 1);

        assertEquals(1, results.size());
        assertEquals(10L, results.get(0).getId());
        assertTrue(results.get(0).getActive());
    }

    @Test
    @DisplayName("3. Similar Sarees: Traversal returns candidates matching structural taxonomy")
    void getSimilarSarees_hydratesFromGraph() {
        when(graphService.findSimilarSarees(eq(5L), anyInt())).thenReturn(List.of(7L, 8L, 9L));

        Product p7 = Product.builder().id(7L).name("Saree 7").price(BigDecimal.valueOf(7000)).active(true).build();
        Product p8 = Product.builder().id(8L).name("Saree 8").price(BigDecimal.valueOf(8000)).active(true).build();
        Product p9 = Product.builder().id(9L).name("Saree 9").price(BigDecimal.valueOf(9000)).active(true).build();

        when(productRepository.findAllById(List.of(7L, 8L, 9L))).thenReturn(List.of(p7, p8, p9));

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

        Product p12 = Product.builder().id(12L).name("Weave 12").price(BigDecimal.valueOf(11000)).active(true).build();
        Product p15 = Product.builder().id(15L).name("Weave 15").price(BigDecimal.valueOf(14000)).active(true).build();

        when(productRepository.findAllById(List.of(12L, 15L))).thenReturn(List.of(p12, p15));

        List<ProductResponse> results = recommendationService.getPersonalizedRecommendations(100L, 2);

        assertEquals(2, results.size());
        assertEquals(12L, results.get(0).getId());
        assertEquals(15L, results.get(1).getId());
    }

    @Test
    @DisplayName("5. Personalized Recommendations for Guest: Gracefully defaults to MySQL active catalog")
    void getPersonalizedRecommendations_forGuestUser() {
        Product p = Product.builder().id(1L).name("Bestseller Saree").price(BigDecimal.valueOf(9999)).active(true).build();
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(p)));

        List<ProductResponse> results = recommendationService.getPersonalizedRecommendations(null, 4);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
        verify(graphService, never()).findPersonalizedRecommendations(any(), anyInt());
    }
}
