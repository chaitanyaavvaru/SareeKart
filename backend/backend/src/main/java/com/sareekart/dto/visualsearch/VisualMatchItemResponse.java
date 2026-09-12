package com.sareekart.dto.visualsearch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualMatchItemResponse {
    private Long id;
    private String name;
    private String fabric;
    private String color;
    private BigDecimal price;
    private String image;
    private String confidence;
    private double confidenceScore;
    private String matchReason;
    private String occasion;
    private Integer stock;
}
