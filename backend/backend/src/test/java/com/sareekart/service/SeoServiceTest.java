package com.sareekart.service;

import com.sareekart.dto.projection.CategorySitemapProjection;
import com.sareekart.dto.projection.ProductSitemapProjection;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.SeoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeoServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SeoServiceImpl seoService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seoService, "canonicalDomain", "https://sareekart.com");
    }

    @Test
    @DisplayName("Should generate valid XML with sitemaps.org namespace and correct declaration")
    void testValidXmlStructure() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertNotNull(xml);
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"));
        assertTrue(xml.endsWith("</urlset>"));
    }

    @Test
    @DisplayName("Should include homepage, catalog root, and public content pages")
    void testIncludesStandardPublicPages() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertTrue(xml.contains("<loc>https://sareekart.com/</loc>"));
        assertTrue(xml.contains("<priority>1.0</priority>"));

        assertTrue(xml.contains("<loc>https://sareekart.com/products</loc>"));
        assertTrue(xml.contains("<priority>0.9</priority>"));

        assertTrue(xml.contains("<loc>https://sareekart.com/heritage-weaves</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/stores</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/artisans</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/saree-care</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/stylist</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/trousseau-planner</loc>"));
    }

    @Test
    @DisplayName("Should include active projected categories and products with lastmod timestamps")
    void testIncludesActiveCategoriesAndProducts() {
        LocalDateTime sampleTime = LocalDateTime.of(2026, 9, 18, 12, 0, 0);

        CategorySitemapProjection catProj = new CategorySitemapProjection() {
            @Override public String getSlug() { return "kanchipuram-silk"; }
            @Override public String getName() { return "Kanchipuram Silk"; }
            @Override public LocalDateTime getUpdatedAt() { return sampleTime; }
        };

        ProductSitemapProjection prodProj = new ProductSitemapProjection() {
            @Override public Long getId() { return 42L; }
            @Override public LocalDateTime getUpdatedAt() { return sampleTime; }
            @Override public LocalDateTime getCreatedAt() { return sampleTime; }
        };

        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(List.of(catProj));
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(List.of(prodProj));

        String xml = seoService.generateSitemapXml();

        assertTrue(xml.contains("<loc>https://sareekart.com/products?category=kanchipuram-silk</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/products/42</loc>"));
        assertTrue(xml.contains("<lastmod>2026-09-18T12:00:00Z</lastmod>"));
        assertTrue(xml.contains("<changefreq>weekly</changefreq>"));
        assertTrue(xml.contains("<priority>0.7</priority>"));
    }

    @Test
    @DisplayName("Should never include private, customer, admin, or tokenized trousseau share routes")
    void testStrictlyExcludesPrivateRoutes() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertFalse(xml.contains("/login"));
        assertFalse(xml.contains("/register"));
        assertFalse(xml.contains("/cart"));
        assertFalse(xml.contains("/checkout"));
        assertFalse(xml.contains("/orders"));
        assertFalse(xml.contains("/wishlist"));
        assertFalse(xml.contains("/wallet"));
        assertFalse(xml.contains("/admin"));
        assertFalse(xml.contains("/trousseau/share"));
        assertFalse(xml.contains("/api/"));
    }

    @Test
    @DisplayName("Should contain zero localhost or staging URLs in canonical sitemap")
    void testNoLocalhostUrls() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertFalse(xml.contains("localhost"));
        assertFalse(xml.contains("127.0.0.1"));
        assertFalse(xml.contains(":8081"));
        assertFalse(xml.contains(":5173"));
    }

    @Test
    @DisplayName("Should handle empty catalog safely without exceptions")
    void testEmptyCatalogSafeHandling() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            String xml = seoService.generateSitemapXml();
            assertNotNull(xml);
            assertTrue(xml.contains("<loc>https://sareekart.com/</loc>"));
        });
    }

    @Test
    @DisplayName("Should generate standard production robots.txt with disallows and sitemap reference")
    void testRobotsTxtDirectives() {
        String robots = seoService.generateRobotsTxt();

        assertNotNull(robots);
        assertTrue(robots.contains("User-agent: *"));
        assertTrue(robots.contains("Allow: /"));
        assertTrue(robots.contains("Allow: /products"));
        assertTrue(robots.contains("Disallow: /admin"));
        assertTrue(robots.contains("Disallow: /checkout"));
        assertTrue(robots.contains("Disallow: /cart"));
        assertTrue(robots.contains("Disallow: /trousseau"));
        assertTrue(robots.contains("Disallow: /api/"));
        assertTrue(robots.contains("Sitemap: https://sareekart.com/sitemap.xml"));
    }
}
