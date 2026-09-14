package com.sareekart.dto.response.customer;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopTrendingProductDto {
    private Long productId;
    private String productName;
    private String category;
    private long viewCount;
    private long cartAddCount;
    private double cartAddRate;
}
