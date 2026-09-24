package com.sareekart.service.impl;

import com.sareekart.entity.Product;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.MetaCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaCatalogServiceImpl implements MetaCatalogService {

    private final ProductRepository productRepository;

    @Value("${app.canonical-domain:https://sareekart.com}")
    private String canonicalDomain = "https://sareekart.com";

    private static final String GOOGLE_PRODUCT_CATEGORY =
            "Apparel & Accessories > Clothing > Traditional & Ceremonial Clothing > Sarees";
    private static final String BRAND = "SareeKart";
    private static final String CONDITION = "new";

    @Override
    @Transactional(readOnly = true)
    public String generateCatalogCsv() {
        String baseUrl = sanitizeBaseUrl(canonicalDomain);
        List<Product> products = productRepository.findByActiveTrue();

        StringBuilder csv = new StringBuilder();
        // Standard Meta Commerce Catalog Header
        csv.append("id,title,description,availability,condition,price,link,image_link,brand,google_product_category,product_type,additional_image_link\r\n");

        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }

            String id = String.valueOf(product.getId());
            String title = (product.getName() != null && !product.getName().isBlank())
                    ? product.getName().trim()
                    : "Luxury Handloom Saree";

            String description = (product.getDescription() != null && !product.getDescription().isBlank())
                    ? product.getDescription().trim()
                    : "Authentic handwoven " + title + " crafted by master weavers at SareeKart.";

            boolean isInStock = product.getStockQuantity() != null && product.getStockQuantity() > 0;
            String availability = isInStock ? "in stock" : "out of stock";

            String price = product.getPrice() != null
                    ? String.format(Locale.US, "%.2f INR", product.getPrice())
                    : "0.00 INR";

            String link = baseUrl + "/products/" + product.getId();

            List<String> images = product.getImages();
            String mainImage = baseUrl + "/images/placeholder-saree.jpg";
            String additionalImages = "";

            if (images != null && !images.isEmpty()) {
                String firstImg = images.get(0);
                if (firstImg != null && !firstImg.isBlank()) {
                    mainImage = toAbsoluteUrl(firstImg, baseUrl);
                }

                if (images.size() > 1) {
                    List<String> addList = new ArrayList<>();
                    for (int i = 1; i < images.size(); i++) {
                        String addImg = images.get(i);
                        if (addImg != null && !addImg.isBlank()) {
                            addList.add(toAbsoluteUrl(addImg, baseUrl));
                        }
                    }
                    if (!addList.isEmpty()) {
                        additionalImages = String.join(",", addList);
                    }
                }
            }

            String productType = "Traditional Sarees";
            if (product.getCategory() != null && product.getCategory().getName() != null && !product.getCategory().getName().isBlank()) {
                productType = product.getCategory().getName().trim();
            }
            if (product.getFabric() != null && !product.getFabric().isBlank()) {
                productType = productType + " > " + product.getFabric().trim();
            }

            // Append RFC 4180 escaped row
            csv.append(escapeCsvField(id)).append(",")
               .append(escapeCsvField(title)).append(",")
               .append(escapeCsvField(description)).append(",")
               .append(escapeCsvField(availability)).append(",")
               .append(escapeCsvField(CONDITION)).append(",")
               .append(escapeCsvField(price)).append(",")
               .append(escapeCsvField(link)).append(",")
               .append(escapeCsvField(mainImage)).append(",")
               .append(escapeCsvField(BRAND)).append(",")
               .append(escapeCsvField(GOOGLE_PRODUCT_CATEGORY)).append(",")
               .append(escapeCsvField(productType)).append(",")
               .append(escapeCsvField(additionalImages))
               .append("\r\n");
        }

        return csv.toString();
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

    private String toAbsoluteUrl(String url, String baseUrl) {
        if (url == null || url.isBlank()) {
            return baseUrl + "/images/placeholder-saree.jpg";
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return baseUrl + trimmed;
        }
        return baseUrl + "/" + trimmed;
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        boolean mustQuote = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r");
        if (mustQuote) {
            String escaped = field.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return field;
    }
}
