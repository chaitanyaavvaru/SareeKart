package com.sareekart.service.impl;

import com.sareekart.dto.projection.CategorySitemapProjection;
import com.sareekart.dto.projection.ProductSitemapProjection;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.SeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeoServiceImpl implements SeoService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.canonical-domain:https://sareekart.com}")
    private String canonicalDomain = "https://sareekart.com";

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final List<String> STATIC_PUBLIC_PATHS = List.of(
            "/heritage-weaves",
            "/stores",
            "/artisans",
            "/saree-care",
            "/stylist",
            "/trousseau-planner"
    );

    @Override
    @Transactional(readOnly = true)
    public String generateSitemapXml() {
        String baseUrl = sanitizeBaseUrl(canonicalDomain);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 1. Storefront Homepage
        appendUrl(xml, baseUrl + "/", null, "daily", "1.0");

        // 2. Main Catalog
        appendUrl(xml, baseUrl + "/products", null, "daily", "0.9");

        // 3. Active Categories (Projected)
        try {
            List<CategorySitemapProjection> categories = categoryRepository.findSitemapProjectionsByActiveTrue();
            for (CategorySitemapProjection category : categories) {
                String identifier = (category.getSlug() != null && !category.getSlug().isBlank())
                        ? category.getSlug()
                        : category.getName();
                if (identifier != null && !identifier.isBlank()) {
                    String encodedParam = URLEncoder.encode(identifier, StandardCharsets.UTF_8).replace("+", "%20");
                    String catUrl = baseUrl + "/products?category=" + encodedParam;
                    appendUrl(xml, catUrl, category.getUpdatedAt(), "daily", "0.8");
                }
            }
        } catch (Exception ex) {
            log.error("Failed to query categories for XML sitemap: {}", ex.getMessage());
        }

        // 4. Active Products (Projected, No N+1)
        try {
            List<ProductSitemapProjection> products = productRepository.findSitemapProjectionsByActiveTrue();
            for (ProductSitemapProjection product : products) {
                String productUrl = baseUrl + "/products/" + product.getId();
                LocalDateTime lastMod = product.getUpdatedAt() != null ? product.getUpdatedAt() : product.getCreatedAt();
                appendUrl(xml, productUrl, lastMod, "weekly", "0.7");
            }
        } catch (Exception ex) {
            log.error("Failed to query products for XML sitemap: {}", ex.getMessage());
        }

        // 5. Static Public Brand, Artisan & Care Pages
        for (String staticPath : STATIC_PUBLIC_PATHS) {
            appendUrl(xml, baseUrl + staticPath, null, "monthly", "0.5");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    @Override
    public String generateRobotsTxt() {
        String baseUrl = sanitizeBaseUrl(canonicalDomain);
        return "# ====================================================================\n" +
                "# SareeKart Enterprise Production Robots Directives\n" +
                "# ====================================================================\n" +
                "User-agent: *\n" +
                "Allow: /\n" +
                "Allow: /products\n" +
                "Allow: /products/*\n" +
                "Allow: /heritage-weaves\n" +
                "Allow: /stores\n" +
                "Allow: /artisans\n" +
                "Allow: /saree-care\n" +
                "Allow: /stylist\n" +
                "Allow: /trousseau-planner\n" +
                "Allow: /assets/\n" +
                "Allow: /uploads/\n" +
                "\n" +
                "# Disallow private customer areas, authentication, checkout, and admin\n" +
                "Disallow: /login\n" +
                "Disallow: /register\n" +
                "Disallow: /forgot-password\n" +
                "Disallow: /reset-password\n" +
                "Disallow: /profile\n" +
                "Disallow: /account\n" +
                "Disallow: /cart\n" +
                "Disallow: /checkout\n" +
                "Disallow: /orders\n" +
                "Disallow: /orders/\n" +
                "Disallow: /track-order\n" +
                "Disallow: /wishlist\n" +
                "Disallow: /wallet\n" +
                "Disallow: /admin\n" +
                "Disallow: /admin/\n" +
                "Disallow: /trousseau\n" +
                "Disallow: /trousseau/\n" +
                "Disallow: /api/\n" +
                "\n" +
                "# Canonical Sitemap\n" +
                "Sitemap: " + baseUrl + "/sitemap.xml\n";
    }

    private void appendUrl(StringBuilder xml, String loc, LocalDateTime lastMod, String changeFreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastMod != null) {
            xml.append("    <lastmod>").append(lastMod.format(ISO_FORMATTER)).append("</lastmod>\n");
        }
        if (changeFreq != null) {
            xml.append("    <changefreq>").append(changeFreq).append("</changefreq>\n");
        }
        if (priority != null) {
            xml.append("    <priority>").append(priority).append("</priority>\n");
        }
        xml.append("  </url>\n");
    }

    private String sanitizeBaseUrl(String domain) {
        if (domain == null || domain.isBlank()) {
            return "https://sareekart.com";
        }
        String trimmed = domain.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }
}
