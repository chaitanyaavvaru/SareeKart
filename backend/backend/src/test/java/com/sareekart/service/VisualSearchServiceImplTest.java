package com.sareekart.service;

import com.sareekart.dto.visualsearch.VisualMatchItemResponse;
import com.sareekart.dto.visualsearch.VisualMatchResponse;
import com.sareekart.dto.visualsearch.VisualSearchRequest;
import com.sareekart.dto.visualsearch.VisualSearchTelemetryResponse;
import com.sareekart.entity.Product;
import com.sareekart.entity.VisualSearchQuery;
import com.sareekart.repository.ProductRepository;
import com.sareekart.repository.VisualSearchQueryRepository;
import com.sareekart.service.impl.VisualSearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VisualSearchServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VisualSearchQueryRepository visualSearchQueryRepository;

    @InjectMocks
    private VisualSearchServiceImpl visualSearchService;

    private List<Product> mockCatalog;
    private Product sareeRedKanchi;
    private Product sareeGoldBanarasi;
    private Product sareeGreenChanderi;

    @BeforeEach
    void setUp() {
        sareeRedKanchi = Product.builder()
                .id(1L)
                .name("Bridal Crimson Kanchipuram Silk")
                .fabric("Kanchipuram Silk")
                .color("Crimson Red")
                .price(new BigDecimal("28500.00"))
                .occasion("Bridal")
                .stockQuantity(5)
                .active(true)
                .images(List.of("https://images.unsplash.com/photo-1610030469983-98e550d6193c"))
                .build();

        sareeGoldBanarasi = Product.builder()
                .id(2L)
                .name("Mustard Gold Banarasi Brocade")
                .fabric("Banarasi Silk")
                .color("Mustard Gold")
                .price(new BigDecimal("19500.00"))
                .occasion("Festive")
                .stockQuantity(8)
                .active(true)
                .images(List.of("https://images.unsplash.com/photo-1617627143750-d86bc21e42bb"))
                .build();

        sareeGreenChanderi = Product.builder()
                .id(3L)
                .name("Emerald Green Chanderi Zari")
                .fabric("Chanderi")
                .color("Emerald Green")
                .price(new BigDecimal("14500.00"))
                .occasion("Festive")
                .stockQuantity(12)
                .active(true)
                .images(List.of("https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b"))
                .build();

        mockCatalog = new ArrayList<>(List.of(sareeRedKanchi, sareeGoldBanarasi, sareeGreenChanderi));
    }

    @Test
    @DisplayName("Should rank closest color and weave first, and persist query telemetry")
    void testMatchSarees_Success_WithColorAndWeave() {
        when(productRepository.findByActiveTrue()).thenReturn(mockCatalog);
        when(visualSearchQueryRepository.save(any(VisualSearchQuery.class))).thenAnswer(i -> i.getArgument(0));

        VisualSearchRequest request = VisualSearchRequest.builder()
                .primaryColor("#B84F49") // Crimson Red Hex
                .secondaryColor("#D4AF37") // Gold Hex
                .weaveHint("Kanchipuram")
                .occasion("Bridal")
                .source("CAMERA")
                .build();

        VisualMatchResponse response = visualSearchService.matchSarees(request, 42L);

        assertNotNull(response);
        assertNotNull(response.getMatches());
        assertEquals(3, response.getMatches().size());

        // Top match should be the Crimson Kanchipuram Saree
        VisualMatchItemResponse topMatch = response.getMatches().get(0);
        assertEquals(1L, topMatch.getId());
        assertEquals("Bridal Crimson Kanchipuram Silk", topMatch.getName());
        assertTrue(topMatch.getConfidenceScore() >= 90.0);
        assertTrue(topMatch.getConfidence().contains("% Match"));
        assertNotNull(topMatch.getMatchReason());

        verify(visualSearchQueryRepository, times(1)).save(any(VisualSearchQuery.class));
    }

    @Test
    @DisplayName("Should gracefully handle empty or null attributes with default palette")
    void testMatchSarees_FallbackDefault_WhenInputEmpty() {
        when(productRepository.findByActiveTrue()).thenReturn(mockCatalog);

        VisualSearchRequest request = VisualSearchRequest.builder()
                .primaryColor(null)
                .secondaryColor(null)
                .weaveHint("")
                .build();

        VisualMatchResponse response = visualSearchService.matchSarees(request, null);

        assertNotNull(response);
        assertFalse(response.getMatches().isEmpty());
        assertNotNull(response.getSummary());
        assertTrue(response.getExecutionTimeMs() > 0);
    }

    @Test
    @DisplayName("Should return visually similar drapes on Product Detail Page excluding self")
    void testFindSimilarDrapes_Success() {
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(sareeRedKanchi));
        when(productRepository.findByActiveTrue()).thenReturn(mockCatalog);

        List<VisualMatchItemResponse> similar = visualSearchService.findSimilarDrapes(1L, 2);

        assertNotNull(similar);
        assertEquals(2, similar.size());
        // Verify self (1L) is not present in similar drapes
        for (VisualMatchItemResponse item : similar) {
            assertNotEquals(1L, item.getId());
        }
    }

    @Test
    @DisplayName("Should return empty list when product ID not found for similar drapes")
    void testFindSimilarDrapes_ProductNotFound() {
        when(productRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        List<VisualMatchItemResponse> similar = visualSearchService.findSimilarDrapes(999L, 4);

        assertNotNull(similar);
        assertTrue(similar.isEmpty());
    }

    @Test
    @DisplayName("Should compute correct visual search telemetry metrics and recent logs")
    void testGetVisualSearchTelemetry_Success() {
        when(visualSearchQueryRepository.count()).thenReturn(150L);
        when(visualSearchQueryRepository.countByCreatedAtAfter(any())).thenReturn(24L);
        when(visualSearchQueryRepository.findAverageConfidenceScore()).thenReturn(95.4);

        VisualSearchQuery q1 = VisualSearchQuery.builder()
                .id(1L)
                .source("CAMERA")
                .extractedPrimaryColor("Crimson Red")
                .extractedWeaveType("Kanchipuram Silk")
                .topMatchedProductId(1L)
                .confidenceScore(new BigDecimal("96.50"))
                .executionTimeMs(12)
                .createdAt(LocalDateTime.now())
                .build();

        when(visualSearchQueryRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(q1));

        List<Object[]> topColors = List.of(
                new Object[]{"Crimson Red", 45L},
                new Object[]{"Emerald Green", 32L}
        );
        when(visualSearchQueryRepository.findTopQueriedColors()).thenReturn(topColors);

        VisualSearchTelemetryResponse telemetry = visualSearchService.getVisualSearchTelemetry();

        assertNotNull(telemetry);
        assertEquals(150L, telemetry.getTotalSearches());
        assertEquals(24L, telemetry.getSearchesToday());
        assertEquals(95.4, telemetry.getAvgConfidenceScore());
        assertEquals(1, telemetry.getRecentLogs().size());
        assertEquals("VS-1001", telemetry.getRecentLogs().get(0).getId());
        assertEquals("Camera Capture", telemetry.getRecentLogs().get(0).getSource());
        assertEquals(2, telemetry.getTopColors().size());
    }
}
