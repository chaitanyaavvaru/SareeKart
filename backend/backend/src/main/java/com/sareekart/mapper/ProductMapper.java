package com.sareekart.mapper;

import com.sareekart.dto.response.ProductResponse;
import com.sareekart.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        Long fabricId = product.getFabricEntity() != null ? product.getFabricEntity().getId() : null;
        Long occasionId = product.getOccasionEntity() != null ? product.getOccasionEntity().getId() : null;
        Long colorId = product.getColorEntity() != null ? product.getColorEntity().getId() : null;

        String colorHex = product.getColorEntity() != null ? product.getColorEntity().getHexCode() : null;
        String colorFamily = product.getColorEntity() != null ? product.getColorEntity().getFamily() : null;

        // Backward compatibility: preserve legacy string or resolve from entity
        String fabricStr = product.getFabric() != null ? product.getFabric() : (product.getFabricEntity() != null ? product.getFabricEntity().getName() : null);
        String occasionStr = product.getOccasion() != null ? product.getOccasion() : (product.getOccasionEntity() != null ? product.getOccasionEntity().getName() : null);
        String colorStr = product.getColor() != null ? product.getColor() : (product.getColorEntity() != null ? product.getColorEntity().getName() : null);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .stockQuantity(product.getStockQuantity())
                .images(product.getImages())
                .fabricId(fabricId)
                .occasionId(occasionId)
                .colorId(colorId)
                .colorHex(colorHex)
                .colorFamily(colorFamily)
                .fabric(fabricStr)
                .occasion(occasionStr)
                .color(colorStr)
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
