package com.sareekart.dto.trousseau;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrousseauItemResponse {

    private Long id;
    private Long boardId;
    private Long ceremonyId;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private String fabric;
    private String color;
    private Integer stockQuantity;
    private Boolean isAiRecommended;
    private String notes;
    private String status;

    @Builder.Default
    private long loveCount = 0;

    @Builder.Default
    private long likeCount = 0;

    @Builder.Default
    private long passCount = 0;

    @Builder.Default
    private List<TrousseauVoteResponse> votes = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
