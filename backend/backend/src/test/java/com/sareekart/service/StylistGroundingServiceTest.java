package com.sareekart.service;

import com.sareekart.dto.internal.StylistIntent;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.dto.response.ScoredProductResponse;
import com.sareekart.entity.Product;
import com.sareekart.mapper.ProductMapper;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.StylistGroundingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StylistGroundingService Unit Tests")
public class StylistGroundingServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private HybridRankingService hybridRankingService;

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private StylistGroundingServiceImpl groundingService;

    private Product product1;
    private Product product2;
    private ProductResponse response1;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .id(101L)
                .name("Kanchipuram Brocade Saree")
                .fabric("Kanchipuram Silk")
                .color("Maroon")
                .price(new BigDecimal("14999.00"))
                .stockQuantity(5)
                .active(true)
                .build();

        product2 = Product.builder()
                .id(102L)
                .name("Banarasi Georgette Saree")
                .fabric("Georgette")
                .color("Emerald Green")
                .price(new BigDecimal("8999.00"))
                .stockQuantity(3)
                .active(true)
                .build();

        response1 = ProductResponse.builder()
                .id(101L)
                .name("Kanchipuram Brocade Saree")
                .fabric("Kanchipuram Silk")
                .color("Maroon")
                .price(new BigDecimal("14999.00"))
                .stockQuantity(5)
                .build();
    }

    @Test
    @DisplayName("Should retrieve candidates matching criteria and rank them via HybridRankingService")
    void testRetrieveGroundedCandidatesStandard() {
        StylistIntent intent = StylistIntent.builder()
                .queryType(StylistIntent.QueryType.FIND_SAREE)
                .occasion("Wedding")
                .maxPrice(new BigDecimal("20000"))
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product1, product2)));

        ScoredProductResponse scored = ScoredProductResponse.builder()
                .product(response1)
                .recommendationScore(0.92)
                .reasons(List.of("Matches wedding occasion"))
                .build();

        when(hybridRankingService.rankCandidates(anyList(), any(), any(), any(), eq("AI_STYLIST"), anyInt()))
                .thenReturn(List.of(scored));

        List<ScoredProductResponse> result = groundingService.retrieveGroundedCandidates(intent, "sess-1", 1L, 6);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getProduct().getId());
        verify(hybridRankingService).rankCandidates(anyList(), any(), any(), any(), eq("AI_STYLIST"), eq(6));
    }

    @Test
    @DisplayName("Should retrieve cheaper alternatives using vector search when SIMILAR_CHEAPER")
    void testRetrieveSimilarCheaperCandidates() {
        Product expensiveRef = Product.builder()
                .id(200L)
                .name("Heirloom Pure Gold Zari Saree")
                .price(new BigDecimal("35000.00"))
                .stockQuantity(2)
                .active(true)
                .build();

        when(productRepository.findByIdAndActiveTrue(200L)).thenReturn(Optional.of(expensiveRef));
        when(vectorSearchService.findSimilarProducts(200L, 25)).thenReturn(List.of(101L, 102L));
        when(productRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(product1, product2));

        when(productMapper.toResponse(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return ProductResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .price(p.getPrice())
                    .fabric(p.getFabric())
                    .color(p.getColor())
                    .stockQuantity(p.getStockQuantity())
                    .build();
        });

        StylistIntent intent = StylistIntent.builder()
                .queryType(StylistIntent.QueryType.SIMILAR_CHEAPER)
                .referenceProductId(200L)
                .build();

        List<ScoredProductResponse> result = groundingService.retrieveGroundedCandidates(intent, "sess-1", null, 4);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // All returned products must have price < 35,000
        for (ScoredProductResponse r : result) {
            assertTrue(r.getProduct().getPrice().compareTo(expensiveRef.getPrice()) < 0);
        }
    }

    @Test
    @DisplayName("Should discard out-of-stock and inactive products from verified list")
    void testGetVerifiedActiveProducts() {
        Product outOfStock = Product.builder()
                .id(103L)
                .name("Sold Out Saree")
                .stockQuantity(0)
                .active(true)
                .build();

        Product inactive = Product.builder()
                .id(104L)
                .name("Deactivated Saree")
                .stockQuantity(10)
                .active(false)
                .build();

        when(productRepository.findAllById(List.of(101L, 103L, 104L)))
                .thenReturn(List.of(product1, outOfStock, inactive));

        List<Product> verified = groundingService.getVerifiedActiveProducts(List.of(101L, 103L, 104L));

        assertEquals(1, verified.size());
        assertEquals(101L, verified.get(0).getId());
    }
}
