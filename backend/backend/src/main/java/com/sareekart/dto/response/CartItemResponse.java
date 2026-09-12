package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;

    // Phase 4 Live Stock & Availability Telemetry
    private Integer availableStock;
    private Boolean isActive;
    private Boolean isOutOfStock;
    private Boolean quantityExceedsStock;
}
