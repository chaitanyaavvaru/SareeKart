package com.sareekart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.entity.CustomerEvent;
import com.sareekart.repository.CustomerEventRepository;
import com.sareekart.service.impl.CustomerTasteVectorServiceImpl;
import com.sareekart.service.impl.VectorSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerTasteVectorServiceTest {

    @Mock
    private CustomerEventRepository customerEventRepository;

    @Mock
    private SareeEmbeddingService sareeEmbeddingService;

    private VectorSearchServiceImpl vectorSearchService;
    private CustomerTasteVectorServiceImpl customerTasteVectorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        vectorSearchService = new VectorSearchServiceImpl();
        customerTasteVectorService = new CustomerTasteVectorServiceImpl(
                customerEventRepository,
                vectorSearchService,
                sareeEmbeddingService,
                objectMapper
        );
    }

    @Test
    @DisplayName("1. Cold Start: User with no events returns Optional.empty()")
    void computeCustomerTasteVector_coldStartReturnsEmpty() {
        when(customerEventRepository.findByUserIdOrderByCreatedAtDesc(eq(999L), any(Pageable.class)))
                .thenReturn(List.of());

        Optional<float[]> tasteOpt = customerTasteVectorService.computeCustomerTasteVector(999L, null);
        assertTrue(tasteOpt.isEmpty());
    }

    @Test
    @DisplayName("2. Active User: Ingests order and cart events to produce normalized taste vector")
    void computeCustomerTasteVector_producesNormalizedVector() {
        // Register embeddings for products 1 and 2
        float[] p1Vec = new float[384];
        p1Vec[0] = 1.0f;
        vectorSearchService.registerEmbedding(1L, p1Vec);

        float[] p2Vec = new float[384];
        p2Vec[1] = 1.0f;
        vectorSearchService.registerEmbedding(2L, p2Vec);

        CustomerEvent orderEvt = CustomerEvent.builder()
                .entityId(1L)
                .eventType("ORDER_COMPLETED")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        CustomerEvent cartEvt = CustomerEvent.builder()
                .entityId(2L)
                .eventType("ADD_TO_CART")
                .createdAt(LocalDateTime.now().minusHours(5))
                .build();

        when(customerEventRepository.findByUserIdOrderByCreatedAtDesc(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(orderEvt, cartEvt));

        Optional<float[]> tasteOpt = customerTasteVectorService.computeCustomerTasteVector(10L, null);

        assertTrue(tasteOpt.isPresent());
        float[] taste = tasteOpt.get();
        assertEquals(384, taste.length);

        // Product 1 (ORDER: weight 5.0) should have larger component than Product 2 (CART: weight 3.0)
        assertTrue(taste[0] > taste[1], "Order event should exert higher weight than cart event");

        // Verify unit norm
        double normSq = 0.0;
        for (float v : taste) normSq += v * v;
        assertEquals(1.0, Math.sqrt(normSq), 0.001);
    }

    @Test
    @DisplayName("3. Recency Decay: Recent interactions have strictly greater impact than older ones")
    void computeCustomerTasteVector_recencyDecayApplied() {
        float[] p1Vec = new float[384];
        p1Vec[0] = 1.0f;
        vectorSearchService.registerEmbedding(1L, p1Vec);

        float[] p2Vec = new float[384];
        p2Vec[1] = 1.0f;
        vectorSearchService.registerEmbedding(2L, p2Vec);

        // Identical event types, but p1 is today, p2 was 28 days ago (2 half-lives)
        CustomerEvent recentEvt = CustomerEvent.builder()
                .entityId(1L)
                .eventType("ADD_TO_CART")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        CustomerEvent oldEvt = CustomerEvent.builder()
                .entityId(2L)
                .eventType("ADD_TO_CART")
                .createdAt(LocalDateTime.now().minusDays(28))
                .build();

        when(customerEventRepository.findByUserIdOrderByCreatedAtDesc(eq(20L), any(Pageable.class)))
                .thenReturn(List.of(recentEvt, oldEvt));

        Optional<float[]> tasteOpt = customerTasteVectorService.computeCustomerTasteVector(20L, null);
        assertTrue(tasteOpt.isPresent());
        float[] taste = tasteOpt.get();

        // Recent event (1 hour ago) must heavily dominate older event (28 days ago = 25% original weight)
        assertTrue(taste[0] > taste[1] * 2.0);
    }

    @Test
    @DisplayName("4. Guest Session: Computes dynamic centroid from sessionId events")
    void computeCustomerTasteVector_forGuestSession() {
        float[] p1Vec = new float[384];
        p1Vec[10] = 1.0f;
        vectorSearchService.registerEmbedding(5L, p1Vec);

        CustomerEvent viewEvt = CustomerEvent.builder()
                .entityId(5L)
                .eventType("PRODUCT_VIEW")
                .metadata("{\"dwellTimeMs\": 45000}")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(customerEventRepository.findBySessionIdOrderByCreatedAtDesc(eq("sess_abc123"), any(Pageable.class)))
                .thenReturn(List.of(viewEvt));

        Optional<float[]> tasteOpt = customerTasteVectorService.computeCustomerTasteVector(null, "sess_abc123");
        assertTrue(tasteOpt.isPresent());
        assertEquals(1.0f, tasteOpt.get()[10], 0.001f);
    }
}
