package com.sareekart.service;

import com.sareekart.controller.MetaCatalogController;
import com.sareekart.entity.Category;
import com.sareekart.entity.Product;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.MetaCatalogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 14 — Stage 7: Meta / Instagram Commerce Readiness Test Suite
 *
 * Verifies all 10 core Meta Commerce catalog scenarios:
 * 1. CSV Header Integrity (id, title, description, availability, condition, price, link, image_link, brand, google_product_category, product_type, additional_image_link).
 * 2. Active product filtering (inactive products excluded from catalog feed).
 * 3. Canonical HTTPS product URLs (zero localhost, ports, or non-canonical domains).
 * 4. Price formatting in INR with standard currency tag.
 * 5. Stock availability mapping (in stock vs out of stock).
 * 6. RFC 4180 CSV escaping for quotes, commas, and line breaks.
 * 7. Absolute HTTPS image resolution and placeholder fallback.
 * 8. Category & fabric hierarchy projection into product_type.
 * 9. Controller endpoint response, UTF-8 charset, and Cache-Control headers.
 * 10. Privacy and zero sensitive data leakage (no customer PII, tokens, or internal credentials).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MetaCatalogProductionVerificationTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MetaCatalogServiceImpl metaCatalogService;

    private MetaCatalogController metaCatalogController;

    private Product activeSareeInStock;
    private Product activeSareeOutOfStock;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(metaCatalogService, "canonicalDomain", "https://sareekart.com");
        metaCatalogController = new MetaCatalogController(metaCatalogService);

        Category banarasiCat = Category.builder()
                .id(101L)
                .name("Banarasi Silk")
                .slug("banarasi-silk")
                .active(true)
                .build();

        activeSareeInStock = Product.builder()
                .id(1L)
                .name("Royal Katan Banarasi Silk Saree")
                .description("Handwoven pure gold zari motifs on mulberry silk.")
                .price(new BigDecimal("18500.00"))
                .stockQuantity(5)
                .category(banarasiCat)
                .fabric("Pure Katan Silk")
                .active(true)
                .images(new ArrayList<>(Arrays.asList(
                        "/uploads/katan-front.jpg",
                        "/uploads/katan-pallu.jpg",
                        "https://cdn.sareekart.com/katan-pleats.jpg"
                )))
                .build();

        activeSareeOutOfStock = Product.builder()
                .id(2L)
                .name("Vintage Uppada Jamdani Saree")
                .description("Ultra-fine translucent weave with silver tissue border.")
                .price(new BigDecimal("24999.00"))
                .stockQuantity(0)
                .category(Category.builder().id(102L).name("Uppada Silk").slug("uppada-silk").active(true).build())
                .fabric("Jamdani Silk Cotton")
                .active(true)
                .images(new ArrayList<>(Collections.singletonList("/uploads/uppada.jpg")))
                .build();
    }

    @Test
    @DisplayName("1. CSV Header Integrity: Meta Commerce Manager 12 required fields in exact order")
    void testCsvHeaderIntegrity() {
        when(productRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        String csv = metaCatalogService.generateCatalogCsv();
        assertNotNull(csv);

        String[] lines = csv.split("\r\n");
        assertTrue(lines.length >= 1, "CSV must contain at least the header row");

        String expectedHeader = "id,title,description,availability,condition,price,link,image_link,brand,google_product_category,product_type,additional_image_link";
        assertEquals(expectedHeader, lines[0]);
    }

    @Test
    @DisplayName("2. Active Product Filtering: Inactive items are excluded from catalog feed")
    void testActiveProductFiltering() {
        when(productRepository.findByActiveTrue()).thenReturn(Collections.singletonList(activeSareeInStock));

        String csv = metaCatalogService.generateCatalogCsv();

        verify(productRepository, times(1)).findByActiveTrue();
        assertTrue(csv.contains("Royal Katan Banarasi Silk Saree"));
        assertFalse(csv.contains("Archived Heritage Saree"));
    }

    @Test
    @DisplayName("3. Canonical HTTPS URLs: Links strictly target https://sareekart.com/products/{id}")
    void testCanonicalProductUrls() {
        when(productRepository.findByActiveTrue()).thenReturn(Collections.singletonList(activeSareeInStock));

        String csv = metaCatalogService.generateCatalogCsv();

        assertTrue(csv.contains("https://sareekart.com/products/1"));
        assertFalse(csv.contains("http://"), "No insecure http URLs permitted");
        assertFalse(csv.contains("localhost"), "No local development hosts permitted");
        assertFalse(csv.contains(":8080"), "No ports permitted in canonical URLs");
        assertFalse(csv.contains(":5173"), "No frontend ports permitted in canonical URLs");
    }

    @Test
    @DisplayName("4. Price Formatting in INR: Formatted with 2 decimals and ISO INR currency suffix")
    void testPriceFormattingInr() {
        when(productRepository.findByActiveTrue()).thenReturn(Arrays.asList(activeSareeInStock, activeSareeOutOfStock));

        String csv = metaCatalogService.generateCatalogCsv();

        assertTrue(csv.contains("18500.00 INR"));
        assertTrue(csv.contains("24999.00 INR"));
    }

    @Test
    @DisplayName("5. Stock Availability Mapping: In-stock vs out-of-stock mapping")
    void testStockAvailabilityMapping() {
        when(productRepository.findByActiveTrue()).thenReturn(Arrays.asList(activeSareeInStock, activeSareeOutOfStock));

        String csv = metaCatalogService.generateCatalogCsv();
        String[] lines = csv.split("\r\n");

        assertEquals(3, lines.length, "Header + 2 products expected");

        // Line 1: activeSareeInStock (stockQuantity = 5) -> "in stock"
        assertTrue(lines[1].contains(",in stock,"));

        // Line 2: activeSareeOutOfStock (stockQuantity = 0) -> "out of stock"
        assertTrue(lines[2].contains(",out of stock,"));
    }

    @Test
    @DisplayName("6. RFC 4180 Escaping: Commas, double quotes, and line breaks are correctly escaped")
    void testRfc4180Escaping() {
        Product specialProduct = Product.builder()
                .id(3L)
                .name("Kanchipuram Silk, \"Bridal Special\" Edition")
                .description("Handcrafted with silver zari,\nwedding collection.")
                .price(new BigDecimal("32000.00"))
                .stockQuantity(2)
                .active(true)
                .build();

        when(productRepository.findByActiveTrue()).thenReturn(Collections.singletonList(specialProduct));

        String csv = metaCatalogService.generateCatalogCsv();

        // Escaped title should wrap in quotes and double the inner quotes
        assertTrue(csv.contains("\"Kanchipuram Silk, \"\"Bridal Special\"\" Edition\""));
        // Escaped description should preserve newline within quoted string
        assertTrue(csv.contains("\"Handcrafted with silver zari,\nwedding collection.\""));
    }

    @Test
    @DisplayName("7. Absolute Image Resolution: Relative paths resolved to HTTPS; fallback placeholder when empty")
    void testAbsoluteImageResolution() {
        Product noImageProduct = Product.builder()
                .id(4L)
                .name("Chanderi Cotton Saree")
                .price(new BigDecimal("4500.00"))
                .stockQuantity(3)
                .active(true)
                .images(Collections.emptyList())
                .build();

        when(productRepository.findByActiveTrue()).thenReturn(Arrays.asList(activeSareeInStock, noImageProduct));

        String csv = metaCatalogService.generateCatalogCsv();

        // Main image resolved with domain
        assertTrue(csv.contains("https://sareekart.com/uploads/katan-front.jpg"));
        // Additional images joined and resolved
        assertTrue(csv.contains("https://sareekart.com/uploads/katan-pallu.jpg,https://cdn.sareekart.com/katan-pleats.jpg"));
        // Missing image product receives fallback placeholder
        assertTrue(csv.contains("https://sareekart.com/images/placeholder-saree.jpg"));
    }

    @Test
    @DisplayName("8. Category & Google Product Taxonomy: Standardized taxonomy mapped")
    void testCategoryAndTaxonomyHierarchy() {
        when(productRepository.findByActiveTrue()).thenReturn(Collections.singletonList(activeSareeInStock));

        String csv = metaCatalogService.generateCatalogCsv();

        // Brand
        assertTrue(csv.contains("SareeKart"));
        // Google product category
        assertTrue(csv.contains("Apparel & Accessories > Clothing > Traditional & Ceremonial Clothing > Sarees"));
        // Product type hierarchy
        assertTrue(csv.contains("Banarasi Silk > Pure Katan Silk"));
    }

    @Test
    @DisplayName("9. Controller Response: Headers, UTF-8 content type, and Cache-Control")
    void testControllerEndpointHeaders() {
        when(productRepository.findByActiveTrue()).thenReturn(Collections.singletonList(activeSareeInStock));

        ResponseEntity<byte[]> response = metaCatalogController.getMetaCatalogCsv();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get(HttpHeaders.CONTENT_TYPE));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("text/csv"));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("UTF-8"));

        assertEquals("public, max-age=3600", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals("inline; filename=\"sareekart-meta-catalog.csv\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue(body.startsWith("id,title,description,"));
    }

    @Test
    @DisplayName("10. Zero Sensitive Data Leakage: No customer, order, token, or auth credentials exposed")
    void testZeroSensitiveDataLeakage() {
        when(productRepository.findByActiveTrue()).thenReturn(Arrays.asList(activeSareeInStock, activeSareeOutOfStock));

        String csv = metaCatalogService.generateCatalogCsv();

        assertFalse(csv.contains("password"), "Must not leak passwords");
        assertFalse(csv.contains("secret"), "Must not leak secrets");
        assertFalse(csv.contains("token"), "Must not leak tokens");
        assertFalse(csv.contains("Bearer"), "Must not leak bearer headers");
        assertFalse(csv.contains("@example.com"), "Must not leak customer email addresses");
        assertFalse(csv.contains("9059564499"), "Must not leak internal admin phone numbers");
    }
}
