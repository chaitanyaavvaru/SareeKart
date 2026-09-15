package com.sareekart.service;

import com.sareekart.controller.RecommendationController;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Category;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.*;
import com.sareekart.service.impl.Neo4jGraphServiceImpl;
import com.sareekart.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Neo4jFailureIsolationTest {

    @Mock
    private Driver driver;

    @Mock
    private Session session;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FabricRepository fabricRepository;

    @Mock
    private OccasionRepository occasionRepository;

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CustomerEventRepository customerEventRepository;

    @Mock
    private GraphSyncFailureRepository graphSyncFailureRepository;

    private Neo4jGraphServiceImpl graphService;
    private RecommendationServiceImpl recommendationService;
    private RecommendationController recommendationController;
    private final ProductMapper productMapper = new ProductMapper();

    @BeforeEach
    void setUp() {
        graphService = new Neo4jGraphServiceImpl(
                driver,
                productRepository,
                categoryRepository,
                fabricRepository,
                occasionRepository,
                colorRepository,
                orderRepository,
                orderItemRepository,
                customerEventRepository,
                graphSyncFailureRepository
        );

        recommendationService = new RecommendationServiceImpl(
                graphService,
                productRepository,
                productMapper
        );

        recommendationController = new RecommendationController(
                recommendationService,
                graphService
        );
    }

    @Test
    @DisplayName("1. Absolute Failure Isolation: When Neo4j is offline, recommendations never crash and fall back to MySQL")
    void recommendations_fallBackToMySQL_whenNeo4jOffline() {
        // Arrange: driver throws ServiceUnavailableException
        when(driver.session()).thenThrow(new ServiceUnavailableException("Neo4j database is down"));

        Category silkCat = Category.builder().id(10L).name("Pure Silk").build();
        Product currentProduct = Product.builder().id(1L).name("Kanchipuram Saree").category(silkCat).active(true).build();
        Product fallbackProduct = Product.builder().id(2L).name("Banarasi Silk").category(silkCat).price(BigDecimal.valueOf(12999)).active(true).build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(currentProduct));
        when(productRepository.findByCategoryIdAndActiveTrue(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fallbackProduct)));

        // Act: call frequently bought together
        List<ProductResponse> results = recommendationService.getFrequentlyBoughtTogether(1L, 4);

        // Assert: commerce continues, fallback product is served with zero fatal exceptions
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getId());
        assertEquals("Banarasi Silk", results.get(0).getName());
    }

    @Test
    @DisplayName("2. Circuit Breaker: Trips OPEN after consecutive failures and bypasses driver calls")
    void circuitBreaker_tripsOpenAfterFailures() {
        when(driver.session()).thenThrow(new ServiceUnavailableException("Connection refused"));

        // Trigger 5 failures to trip circuit
        for (int i = 0; i < 5; i++) {
            assertFalse(graphService.isAvailable());
        }

        // 6th call should be short-circuited without touching driver.session()
        assertFalse(graphService.isAvailable());

        // Driver.session() was only called 5 times, not 6
        verify(driver, times(5)).session();

        Map<String, Object> stats = graphService.getGraphStatistics();
        assertEquals(false, stats.get("available"));
        assertEquals(true, stats.get("circuitOpen"));
    }

    @Test
    @DisplayName("3. Dead-Letter Queue: Unreachable Neo4j persists sync failure to MySQL without throwing")
    void deadLetterQueue_persistsFailure_whenNeo4jWriteFails() {
        when(driver.session()).thenThrow(new ServiceUnavailableException("Write connection failed"));

        // Act: Attempt to record view with failing driver
        assertDoesNotThrow(() -> {
            graphService.recordProductView(42L, 101L, 3000L, "2026-09-15T10:00:00Z");
        });

        // Assert: GraphSyncFailure is saved to repository
        verify(graphSyncFailureRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("4. Controller Non-Blocking Guarantee: Endpoint returns HTTP 200 with fallback data")
    void controller_returns200WithFallback_whenGraphFails() {
        when(driver.session()).thenThrow(new ServiceUnavailableException("Graph unreachable"));

        Product p = Product.builder().id(5L).name("Chanderi Saree").active(true).price(BigDecimal.valueOf(4999)).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(p));
        when(productRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        ResponseEntity<ApiResponse<List<ProductResponse>>> response = recommendationController.getCustomersAlsoViewed(5L, 6);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertNotNull(response.getBody().getData());
    }

    @Test
    @DisplayName("5. Null Driver Resilience: If Neo4j bean is null (disabled in config), service safely degrades")
    void nullDriver_operatesInSafeDegradedMode() {
        Neo4jGraphServiceImpl nullDriverService = new Neo4jGraphServiceImpl(
                null,
                productRepository,
                categoryRepository,
                fabricRepository,
                occasionRepository,
                colorRepository,
                orderRepository,
                orderItemRepository,
                customerEventRepository,
                graphSyncFailureRepository
        );

        assertFalse(nullDriverService.isAvailable());
        assertEquals(Collections.emptyList(), nullDriverService.findFrequentlyBoughtTogether(1L, 4));
        assertEquals(Collections.emptyList(), nullDriverService.findCustomersAlsoViewed(1L, 4));
        assertEquals(Collections.emptyList(), nullDriverService.findPersonalizedRecommendations(1L, 4));
        assertEquals(Collections.emptyList(), nullDriverService.findSimilarSarees(1L, 4));

        Map<String, Object> stats = nullDriverService.getGraphStatistics();
        assertEquals(false, stats.get("available"));
    }
}
