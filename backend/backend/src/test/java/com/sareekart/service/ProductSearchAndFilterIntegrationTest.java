package com.sareekart.service;

import com.sareekart.dto.request.ProductSearchCriteria;
import com.sareekart.dto.response.PagedResponse;
import com.sareekart.dto.response.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ProductSearchAndFilterIntegrationTest {

    @Autowired
    private ProductService productService;

    @Test
    @DisplayName("1. Default query returns active products")
    void testDefaultQueryReturnsProducts() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertTrue(response.getTotalElements() > 0, "Should return catalog products");
        assertTrue(response.getContent().stream().allMatch(ProductResponse::getActive));
    }

    @Test
    @DisplayName("2. Keyword search matches by product name")
    void testKeywordSearchByName() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .q("Kanchipuram")
                .page(0)
                .size(10)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty(), "Should match products containing 'Kanchipuram'");
        assertTrue(response.getContent().stream()
                .anyMatch(p -> p.getName().toLowerCase().contains("kanchipuram")));
    }

    @Test
    @DisplayName("3. Multi-token keyword search (Safeguard 1: AND semantics across tokens)")
    void testMultiTokenKeywordSearch() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .q("silk red")
                .page(0)
                .size(10)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        for (ProductResponse p : response.getContent()) {
            String combined = (p.getName() + " " + p.getDescription() + " " + p.getFabric() + " " + p.getColor() + " " + p.getColorFamily() + " " + p.getCategoryName()).toLowerCase();
            assertTrue(combined.contains("silk"), "Result must match token 'silk'");
            assertTrue(combined.contains("red"), "Result must match token 'red'");
        }
    }

    @Test
    @DisplayName("4. Category hierarchy filtering (Safeguard 2: matches category and direct children)")
    void testCategoryFilter() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .category("silk-sarees")
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty());
    }

    @Test
    @DisplayName("5. Fabric filtering by slug")
    void testFabricFilterBySlug() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .fabric("silk")
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty());
        for (ProductResponse p : response.getContent()) {
            assertTrue(p.getFabric().equalsIgnoreCase("silk") ||
                    (p.getFabricId() != null && p.getFabric().toLowerCase().contains("silk")));
        }
    }

    @Test
    @DisplayName("6. Occasion filtering by slug")
    void testOccasionFilterBySlug() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .occasion("wedding")
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty());
        for (ProductResponse p : response.getContent()) {
            assertTrue(p.getOccasion().equalsIgnoreCase("wedding") ||
                    (p.getOccasionId() != null && p.getOccasion().toLowerCase().contains("wedding")));
        }
    }

    @Test
    @DisplayName("7. Color Family filtering from DB canonical table (Safeguard 3)")
    void testColorFamilyFilter() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .colorFamily("Red")
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty(), "Should find products belonging to Red family");
        for (ProductResponse p : response.getContent()) {
            assertEquals("Red", p.getColorFamily(), "Color family must be Red");
        }
    }

    @Test
    @DisplayName("8. Specific Color filtering by slug")
    void testSpecificColorFilter() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .color("ruby-red")
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        for (ProductResponse p : response.getContent()) {
            assertTrue(p.getColor().equalsIgnoreCase("Ruby Red") ||
                    (p.getColorId() != null && p.getColor().toLowerCase().contains("red")));
        }
    }

    @Test
    @DisplayName("9. Price range filtering (minPrice and maxPrice)")
    void testPriceRangeFilter() {
        BigDecimal min = new BigDecimal("5000.00");
        BigDecimal max = new BigDecimal("25000.00");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .minPrice(min)
                .maxPrice(max)
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty());
        for (ProductResponse p : response.getContent()) {
            assertTrue(p.getPrice().compareTo(min) >= 0, "Price should be >= " + min);
            assertTrue(p.getPrice().compareTo(max) <= 0, "Price should be <= " + max);
        }
    }

    @Test
    @DisplayName("10. In-Stock filtering")
    void testInStockFilter() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .inStock(true)
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertFalse(response.getContent().isEmpty());
        for (ProductResponse p : response.getContent()) {
            assertTrue(p.getStockQuantity() > 0, "Stock quantity must be > 0");
        }
    }

    @Test
    @DisplayName("11. Sorting by price ascending and descending")
    void testSortingByPrice() {
        ProductSearchCriteria ascCriteria = ProductSearchCriteria.builder()
                .sortBy("price")
                .sortDir("asc")
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> ascResponse = productService.searchAndFilterProducts(ascCriteria);
        assertNotNull(ascResponse);
        for (int i = 0; i < ascResponse.getContent().size() - 1; i++) {
            BigDecimal p1 = ascResponse.getContent().get(i).getPrice();
            BigDecimal p2 = ascResponse.getContent().get(i + 1).getPrice();
            assertTrue(p1.compareTo(p2) <= 0, "Prices must be in ascending order: " + p1 + " <= " + p2);
        }

        ProductSearchCriteria descCriteria = ProductSearchCriteria.builder()
                .sortBy("price")
                .sortDir("desc")
                .page(0)
                .size(25)
                .build();

        PagedResponse<ProductResponse> descResponse = productService.searchAndFilterProducts(descCriteria);
        assertNotNull(descResponse);
        for (int i = 0; i < descResponse.getContent().size() - 1; i++) {
            BigDecimal p1 = descResponse.getContent().get(i).getPrice();
            BigDecimal p2 = descResponse.getContent().get(i + 1).getPrice();
            assertTrue(p1.compareTo(p2) >= 0, "Prices must be in descending order: " + p1 + " >= " + p2);
        }
    }

    @Test
    @DisplayName("12. Combined multi-facet search + filter query (Safeguard 4: safe pagination)")
    void testCombinedMultiFacetQuery() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .q("Silk")
                .fabric("silk")
                .colorFamily("Red")
                .inStock(true)
                .sortBy("price")
                .sortDir("asc")
                .page(0)
                .size(10)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        // Verify duplicate-free pagination
        long uniqueIds = response.getContent().stream().map(ProductResponse::getId).distinct().count();
        assertEquals(response.getContent().size(), uniqueIds, "Results must have zero duplicate products");
    }

    @Test
    @DisplayName("13. Non-matching search criteria returns empty response with 0 total elements")
    void testEmptyStateForNonExistentProduct() {
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .q("xyznonexistentterm99999")
                .page(0)
                .size(10)
                .build();

        PagedResponse<ProductResponse> response = productService.searchAndFilterProducts(criteria);
        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getContent().isEmpty());
    }
}
