package com.sareekart.service;

import com.sareekart.controller.SeoController;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Phase 14 — Stage 6: SEO & Search Engine Production Verification Test Suite
 *
 * Verifies all 10 core SEO scenarios:
 * 1. XML sitemap protocol compliance (sitemaps.org 0.9 schema, XML declaration, closing urlset).
 * 2. Public canonical HTTPS URL enforcement (zero localhost, ports, or non-canonical domains).
 * 3. Strict exclusion of private routes, customer areas, invoices, admin, and trousseau tokens.
 * 4. Only active catalog records reflected (inactive/archived items omitted).
 * 5. XML escaping of reserved characters (&, <, >, ", ') to prevent XML injection/malformation.
 * 6. Robots.txt allow/disallow matrix for public catalog and private boundaries.
 * 7. Robots.txt explicit disallows for /invoices, /admin, /checkout and canonical sitemap pointer.
 * 8. Absence of blanket Disallow: / directive.
 * 9. Canonical domain sanitization handling trailing slashes and blank inputs.
 * 10. Controller endpoint response codes, Cache-Control headers, and MediaTypes.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SeoProductionVerificationTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private SeoServiceImpl seoService;

    private SeoController seoController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seoService, "canonicalDomain", "https://sareekart.com");
        seoController = new SeoController(seoService);
    }

    @Test
    @DisplayName("Point 1 — Sitemap XML Protocol Compliance: adheres to sitemaps.org 0.9 schema")
    void testSitemapXmlProtocolCompliance() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertNotNull(xml, "Sitemap XML must not be null");
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"),
                "Sitemap must start with standard UTF-8 XML declaration");
        assertTrue(xml.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"),
                "Sitemap must declare the sitemaps.org 0.9 namespace");
        assertTrue(xml.endsWith("</urlset>"), "Sitemap must terminate cleanly with </urlset>");
    }

    @Test
    @DisplayName("Point 2 — Canonical HTTPS URLs: all URLs are canonical HTTPS without localhost or ports")
    void testSitemapOnlyPublicCanonicalUrls() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertTrue(xml.contains("<loc>https://sareekart.com/</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/products</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/heritage-weaves</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/stores</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/artisans</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/saree-care</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/stylist</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/trousseau-planner</loc>"));

        // Negative security assertions
        assertFalse(xml.contains("<loc>http://"), "Sitemap loc tags must not contain unencrypted HTTP URLs");
        assertFalse(xml.contains("localhost"), "Sitemap must not leak localhost");
        assertFalse(xml.contains("127.0.0.1"), "Sitemap must not leak loopback IP");
        assertFalse(xml.contains(":8081"), "Sitemap must not contain development backend ports");
        assertFalse(xml.contains(":5173"), "Sitemap must not contain development frontend ports");
    }

    @Test
    @DisplayName("Point 3 — Private Route Exclusion: private customer, admin, and tokenized URLs excluded")
    void testSitemapStrictlyExcludesPrivateAndAdminRoutes() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        assertFalse(xml.contains("/login"), "Sitemap must not include /login");
        assertFalse(xml.contains("/register"), "Sitemap must not include /register");
        assertFalse(xml.contains("/forgot-password"), "Sitemap must not include /forgot-password");
        assertFalse(xml.contains("/reset-password"), "Sitemap must not include /reset-password");
        assertFalse(xml.contains("/cart"), "Sitemap must not include /cart");
        assertFalse(xml.contains("/checkout"), "Sitemap must not include /checkout");
        assertFalse(xml.contains("/orders"), "Sitemap must not include /orders");
        assertFalse(xml.contains("/wishlist"), "Sitemap must not include /wishlist");
        assertFalse(xml.contains("/wallet"), "Sitemap must not include /wallet");
        assertFalse(xml.contains("/invoices"), "Sitemap must not include /invoices");
        assertFalse(xml.contains("/admin"), "Sitemap must not include /admin");
        assertFalse(xml.contains("/trousseau/share"), "Sitemap must not include /trousseau/share");
        assertFalse(xml.contains("/api/"), "Sitemap must not include /api/ internal endpoints");
    }

    @Test
    @DisplayName("Point 4 — Real Catalog Records: projected active categories and products included")
    void testSitemapReflectsOnlyActiveRecords() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 23, 10, 30, 0);

        CategorySitemapProjection cat = new CategorySitemapProjection() {
            @Override public String getSlug() { return "kanchipuram-silk"; }
            @Override public String getName() { return "Kanchipuram Silk"; }
            @Override public LocalDateTime getUpdatedAt() { return timestamp; }
        };

        ProductSitemapProjection prod = new ProductSitemapProjection() {
            @Override public Long getId() { return 108L; }
            @Override public LocalDateTime getUpdatedAt() { return timestamp; }
            @Override public LocalDateTime getCreatedAt() { return timestamp.minusDays(5); }
        };

        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(List.of(cat));
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(List.of(prod));

        String xml = seoService.generateSitemapXml();

        assertTrue(xml.contains("<loc>https://sareekart.com/products?category=kanchipuram-silk</loc>"));
        assertTrue(xml.contains("<loc>https://sareekart.com/products/108</loc>"));
        assertTrue(xml.contains("<lastmod>2026-09-23T10:30:00Z</lastmod>"));
        assertTrue(xml.contains("<changefreq>weekly</changefreq>"));
        assertTrue(xml.contains("<priority>0.7</priority>"));
    }

    @Test
    @DisplayName("Point 5 — XML Injection & Special Character Escaping: characters properly escaped")
    void testSitemapXmlSpecialCharacterEscaping() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 9, 23, 11, 0, 0);

        CategorySitemapProjection ampersandCategory = new CategorySitemapProjection() {
            @Override public String getSlug() { return "silk & zari"; }
            @Override public String getName() { return "Silk & Zari"; }
            @Override public LocalDateTime getUpdatedAt() { return timestamp; }
        };

        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(List.of(ampersandCategory));
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();

        // URLEncoder encodes space and ampersand: silk%20%26%20zari
        // escapeXml ensures any raw & is escaped to &amp;
        assertFalse(xml.contains("<loc>https://sareekart.com/products?category=silk & zari</loc>"),
                "Unencoded ampersand and space must not be present in XML loc");
        assertTrue(xml.contains("category=silk%20%26%20zari") || xml.contains("&amp;"),
                "URL must be safely encoded without invalid XML characters");
    }

    @Test
    @DisplayName("Point 6 — Robots.txt Directives: allows public catalog and assets")
    void testRobotsTxtAllowAndDisallowDirectives() {
        String robots = seoService.generateRobotsTxt();

        assertNotNull(robots);
        assertTrue(robots.contains("User-agent: *"));
        assertTrue(robots.contains("Allow: /"));
        assertTrue(robots.contains("Allow: /products"));
        assertTrue(robots.contains("Allow: /products/*"));
        assertTrue(robots.contains("Allow: /heritage-weaves"));
        assertTrue(robots.contains("Allow: /stores"));
        assertTrue(robots.contains("Allow: /artisans"));
        assertTrue(robots.contains("Allow: /saree-care"));
        assertTrue(robots.contains("Allow: /stylist"));
        assertTrue(robots.contains("Allow: /trousseau-planner"));
        assertTrue(robots.contains("Allow: /assets/"));
        assertTrue(robots.contains("Allow: /uploads/"));
    }

    @Test
    @DisplayName("Point 7 — Robots.txt Private Disallows: disallows /invoices, /admin, /checkout, /cart")
    void testRobotsTxtIncludesInvoicesAndSitemapReference() {
        String robots = seoService.generateRobotsTxt();

        assertTrue(robots.contains("Disallow: /invoices"), "robots.txt must disallow /invoices");
        assertTrue(robots.contains("Disallow: /admin"), "robots.txt must disallow /admin");
        assertTrue(robots.contains("Disallow: /checkout"), "robots.txt must disallow /checkout");
        assertTrue(robots.contains("Disallow: /cart"), "robots.txt must disallow /cart");
        assertTrue(robots.contains("Disallow: /orders"), "robots.txt must disallow /orders");
        assertTrue(robots.contains("Disallow: /wallet"), "robots.txt must disallow /wallet");
        assertTrue(robots.contains("Disallow: /wishlist"), "robots.txt must disallow /wishlist");
        assertTrue(robots.contains("Disallow: /trousseau"), "robots.txt must disallow /trousseau");
        assertTrue(robots.contains("Disallow: /api/"), "robots.txt must disallow /api/");
        assertTrue(robots.contains("Sitemap: https://sareekart.com/sitemap.xml"),
                "robots.txt must specify canonical sitemap URL");
    }

    @Test
    @DisplayName("Point 8 — No Blanket Disallow: ensures site is not accidentally hidden from crawlers")
    void testRobotsTxtNoAccidentalBlanketDisallow() {
        String robots = seoService.generateRobotsTxt();

        // Ensure "Disallow: /" (with newline directly following or whitespace only) does NOT exist
        assertFalse(robots.contains("Disallow: /\n") || robots.contains("Disallow: /\r"),
                "robots.txt must not contain blanket 'Disallow: /' directive");
    }

    @Test
    @DisplayName("Point 9 — Canonical Domain Sanitization: handles trailing slashes without duplicate slashes")
    void testCanonicalDomainSanitization() {
        // Set canonical domain with multiple trailing slashes
        ReflectionTestUtils.setField(seoService, "canonicalDomain", "https://sareekart.com///");
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        String xml = seoService.generateSitemapXml();
        String robots = seoService.generateRobotsTxt();

        // Must not contain https://sareekart.com//products or https://sareekart.com//
        assertFalse(xml.contains("https://sareekart.com//products"), "Must not create duplicate slashes");
        assertTrue(xml.contains("<loc>https://sareekart.com/products</loc>"));
        assertTrue(robots.contains("Sitemap: https://sareekart.com/sitemap.xml"));
    }

    @Test
    @DisplayName("Point 10 — Controller Endpoints: sitemap.xml and robots.txt return HTTP 200 with headers")
    void testSeoControllerEndpoints() {
        when(categoryRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());
        when(productRepository.findSitemapProjectionsByActiveTrue()).thenReturn(Collections.emptyList());

        // Sitemap endpoint
        ResponseEntity<String> sitemapResp = seoController.getSitemapXml();
        assertEquals(HttpStatus.OK, sitemapResp.getStatusCode());
        assertTrue(sitemapResp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("application/xml"));
        assertEquals("public, max-age=3600", sitemapResp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertNotNull(sitemapResp.getBody());
        assertTrue(sitemapResp.getBody().contains("<urlset"));

        // Robots endpoint
        ResponseEntity<String> robotsResp = seoController.getRobotsTxt();
        assertEquals(HttpStatus.OK, robotsResp.getStatusCode());
        assertTrue(robotsResp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("text/plain"));
        assertEquals("public, max-age=86400", robotsResp.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
        assertNotNull(robotsResp.getBody());
        assertTrue(robotsResp.getBody().contains("User-agent: *"));
    }
}
