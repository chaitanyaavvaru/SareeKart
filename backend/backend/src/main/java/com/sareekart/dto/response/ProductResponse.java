package com.sareekart.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private Integer stockQuantity;
    private List<String> images;

    // CANONICAL FOREIGN KEYS & METADATA (Phase 2)
    private Long fabricId;
    private Long occasionId;
    private Long colorId;
    private String colorHex;
    private String colorFamily;

    // BACKWARD COMPATIBILITY: Legacy string fields
    private String fabric;
    private String occasion;
    private String color;

    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
