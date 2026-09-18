package com.sareekart.dto.trousseau;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferToCartResponse {

    private Long boardId;
    private Long ceremonyId;

    @Builder.Default
    private int addedCount = 0;

    @Builder.Default
    private int alreadyInCartCount = 0;

    @Builder.Default
    private int outOfStockCount = 0;

    @Builder.Default
    private List<Long> addedProductIds = new ArrayList<>();

    @Builder.Default
    private List<Long> skippedProductIds = new ArrayList<>();

    @Builder.Default
    private List<Long> unavailableProductIds = new ArrayList<>();

    private Integer totalCartItems;
    private BigDecimal cartTotalPrice;

    private String message;
}
