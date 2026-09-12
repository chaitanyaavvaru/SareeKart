package com.sareekart.config;

import com.sareekart.entity.Product;
import com.sareekart.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Configuration
@Slf4j
public class AIToolConfig {

    public record ProductSearchRequest(String query, BigDecimal maxPrice, String fabric) {}
    public record ProductInfo(Long id, String name, BigDecimal price, String imageUrl, String fabric) {}
    public record ProductSearchResponse(List<ProductInfo> products) {}

    @Bean
    public Function<ProductSearchRequest, ProductSearchResponse> searchProducts(ProductRepository productRepository) {
        return request -> {
            log.info("AI invoked searchProducts tool with request: {}", request);
            
            // Simplified search combining query or filters
            List<Product> results;
            if (request.query() != null && !request.query().isEmpty()) {
                results = productRepository.searchProducts(request.query(), PageRequest.of(0, 5)).getContent();
            } else {
                results = productRepository.findByFilters(null, null, request.maxPrice(), request.fabric(), PageRequest.of(0, 5)).getContent();
            }

            List<ProductInfo> dtoList = results.stream()
                    .map(p -> new ProductInfo(
                            p.getId(), 
                            p.getName(), 
                            p.getPrice(), 
                            p.getImages() != null && !p.getImages().isEmpty() ? p.getImages().get(0) : null,
                            p.getFabric()))
                    .toList();
            
            return new ProductSearchResponse(dtoList);
        };
    }
}
