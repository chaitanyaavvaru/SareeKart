package com.sareekart.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Category;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.GraphService;
import com.sareekart.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "neo4j.enabled=false",
        "NEO4J_ENABLED=false",
        "management.health.neo4j.enabled=false"
})
class Neo4jEmpiricalStressTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private Driver driver;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private GraphService graphService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private Long kanchiCatId;
    private Long kanchiProduct1Id;
    private Long kanchiProduct2Id;
    private Long kanchiInactiveId;
    private Long banarasiProduct1Id;

    @BeforeEach
    void setUp() {
        List<Product> products = productRepository.findAll();
        assertFalse(products.isEmpty(), "DataSeeder must have seeded products in the catalog");

        Product sample1 = products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()) && p.getCategory() != null)
                .findFirst()
                .orElseThrow();
        kanchiProduct1Id = sample1.getId();
        kanchiCatId = sample1.getCategory().getId();

        Product sibling = products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()) && p.getCategory() != null
                        && p.getCategory().getId().equals(kanchiCatId) && !p.getId().equals(kanchiProduct1Id))
                .findFirst()
                .orElse(null);

        if (sibling == null) {
            sibling = productRepository.save(Product.builder()
                    .name("Test Sibling Saree")
                    .description("Test Sibling Saree for same category fallback verification")
                    .price(BigDecimal.valueOf(15000))
                    .category(sample1.getCategory())
                    .active(true)
                    .stockQuantity(10)
                    .build());
        }
        kanchiProduct2Id = sibling.getId();

        Product inactive = products.stream()
                .filter(p -> Boolean.FALSE.equals(p.getActive()))
                .findFirst()
                .orElse(null);

        if (inactive == null) {
            inactive = productRepository.save(Product.builder()
                    .name("Test Inactive Saree")
                    .description("Discontinued saree that must never appear in recommendations")
                    .price(BigDecimal.valueOf(9999))
                    .category(sample1.getCategory())
                    .active(false)
                    .stockQuantity(0)
                    .build());
        }
        kanchiInactiveId = inactive.getId();

        Product crossCategory = products.stream()
                .filter(p -> Boolean.TRUE.equals(p.getActive()) && p.getCategory() != null
                        && !p.getCategory().getId().equals(kanchiCatId))
                .findFirst()
                .orElseThrow();
        banarasiProduct1Id = crossCategory.getId();
    }

    @Test
    @DisplayName("Challenge 1: Driver Bean is NULL when NEO4J_ENABLED=false")
    void driverBeanIsNullWhenDisabled() {
        assertNull(driver, "Neo4j Driver bean MUST be null when NEO4J_ENABLED=false");
    }

    @Test
    @DisplayName("Challenge 2: Actuator Health returns 200 OK and UP status when Neo4j is disabled")
    void actuatorHealth_returns200AndUP_whenNeo4jDisabled() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Challenge 3: Actuator Info returns 200 OK when Neo4j is disabled")
    void actuatorInfo_returns200_whenNeo4jDisabled() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Challenge 4: Recommendation status returns 200 OK and available=false")
    void recommendationStatus_returns200AndUnavailable() throws Exception {
        mockMvc.perform(get("/api/recommendations/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false))
                .andExpect(jsonPath("$.data.circuitOpen").value(false))
                .andExpect(jsonPath("$.data.consecutiveFailures").value(0));
    }

    @Test
    @DisplayName("Challenge 5: Frequently Bought Together endpoint serves authentic active MySQL products with zero errors")
    void frequentlyBoughtTogether_servesAuthenticActiveMySQLFallbacks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations/frequently-bought-together/" + kanchiProduct1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertFalse(data.isEmpty(), "Should return recommendations from MySQL catalog");

        List<Long> returnedIds = new ArrayList<>();
        for (JsonNode item : data) {
            returnedIds.add(item.get("id").asLong());
            assertTrue(item.get("active").asBoolean(), "Only active products should be served in fallback");
        }

        assertFalse(returnedIds.contains(kanchiProduct1Id), "Target product must be excluded from its own recommendations");
        assertFalse(returnedIds.contains(kanchiInactiveId), "Inactive product must be excluded from recommendations");
    }

    @Test
    @DisplayName("Challenge 6: Customers Also Viewed serves authentic active MySQL fallback")
    void customersAlsoViewed_servesAuthenticActiveMySQLFallbacks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations/customers-also-viewed/" + kanchiProduct1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertFalse(data.isEmpty());

        for (JsonNode item : data) {
            assertTrue(item.get("active").asBoolean());
            assertNotEquals(kanchiProduct1Id.longValue(), item.get("id").asLong());
            assertNotEquals(kanchiInactiveId.longValue(), item.get("id").asLong());
        }
    }

    @Test
    @DisplayName("Challenge 7: Personalized recommendations for guest user serves active MySQL catalog")
    void personalized_servesActiveCatalogToGuest() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations/personalized"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertFalse(data.isEmpty());

        for (JsonNode item : data) {
            assertTrue(item.get("active").asBoolean());
            assertNotEquals(kanchiInactiveId.longValue(), item.get("id").asLong());
        }
    }

    @Test
    @DisplayName("Challenge 8: Similar Sarees serves authentic active catalog fallback")
    void similarSarees_servesAuthenticCatalog() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations/similar/" + kanchiProduct1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertFalse(data.isEmpty());

        for (JsonNode item : data) {
            assertTrue(item.get("active").asBoolean());
            assertNotEquals(kanchiProduct1Id.longValue(), item.get("id").asLong());
        }
    }

    @Test
    @DisplayName("Challenge 9: Trending Sarees serves active catalog")
    void trendingSarees_servesActiveCatalog() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations/trending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertFalse(data.isEmpty());

        for (JsonNode item : data) {
            assertTrue(item.get("active").asBoolean());
            assertNotEquals(kanchiInactiveId.longValue(), item.get("id").asLong());
        }
    }

    @Test
    @DisplayName("Challenge 10: Complete The Look serves cross-category active catalog")
    void completeTheLook_servesCrossCategoryCatalog() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations/complete-the-look/" + kanchiProduct1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertFalse(data.isEmpty());

        List<Long> returnedIds = new ArrayList<>();
        for (JsonNode item : data) {
            returnedIds.add(item.get("id").asLong());
            assertTrue(item.get("active").asBoolean());
        }
        assertFalse(returnedIds.contains(kanchiProduct1Id));
    }

    @Test
    @DisplayName("Challenge 11: Explainable recommendations return scored products with non-null score and reasons")
    void explainable_servesScoredProductsWithCuratedTag() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations/explainable/" + kanchiProduct1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertFalse(data.isEmpty());
        assertTrue(data.get(0).get("recommendationScore").asDouble() > 0.0);
        assertTrue(data.get(0).get("reasons").isArray());
    }

    @Test
    @DisplayName("Challenge 12: Non-existent product ID gracefully degrades to MySQL fallback without throwing 500")
    void boundary_nonExistentProductId_gracefullyDegradesWithout500() throws Exception {
        mockMvc.perform(get("/api/recommendations/frequently-bought-together/9999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Challenge 13: Non-existent product ID for complete-the-look gracefully falls back to trending")
    void boundary_nonExistentProductId_completeTheLook_gracefullyDegrades() throws Exception {
        mockMvc.perform(get("/api/recommendations/complete-the-look/9999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Challenge 14: Direct fallbackHydrate() contract verification with standalone RecommendationServiceImpl")
    @org.springframework.transaction.annotation.Transactional
    void directFallbackHydrate_servesCategoryFallbacksFromMySQL() {
        // Pure fallback hydration path: hybrid ranker bypassed
        RecommendationServiceImpl pureFallbackService = new RecommendationServiceImpl(
                graphService,
                productRepository,
                productMapper
        );

        List<ProductResponse> fallbacks = pureFallbackService.getFrequentlyBoughtTogether(kanchiProduct1Id, 4);

        assertNotNull(fallbacks);
        assertFalse(fallbacks.isEmpty(), "fallbackHydrate must return products from MySQL");

        List<Long> fallbackIds = fallbacks.stream().map(ProductResponse::getId).toList();
        assertFalse(fallbackIds.contains(kanchiProduct1Id), "Target product must be excluded");
        assertFalse(fallbackIds.contains(kanchiInactiveId), "Inactive product must be excluded");
        assertTrue(fallbackIds.contains(kanchiProduct2Id), "Active sibling in same category must be served by fallbackHydrate");
    }

    @Test
    @DisplayName("Challenge 15: Concurrency Stress Test: 50 concurrent requests against fallback engine execute cleanly")
    void concurrency_stressTestFallbackHydration_underHighLoad() throws Exception {
        int totalRequests = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        String[] endpoints = {
                "/api/recommendations/frequently-bought-together/" + kanchiProduct1Id,
                "/api/recommendations/customers-also-viewed/" + kanchiProduct1Id,
                "/api/recommendations/similar/" + kanchiProduct1Id,
                "/api/recommendations/trending",
                "/api/recommendations/complete-the-look/" + kanchiProduct1Id,
                "/api/recommendations/personalized",
                "/api/recommendations/status",
                "/actuator/health"
        };

        for (int i = 0; i < totalRequests; i++) {
            final String endpoint = endpoints[i % endpoints.length];
            executor.submit(() -> {
                try {
                    mockMvc.perform(get(endpoint))
                            .andExpect(status().isOk());
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 50 concurrent requests should complete within 15 seconds");
        assertEquals(totalRequests, successCount.get(), "All 50 concurrent requests must return HTTP 200");
        assertEquals(0, failureCount.get(), "Zero exceptions or failures permitted under fallback concurrency");
    }
}
