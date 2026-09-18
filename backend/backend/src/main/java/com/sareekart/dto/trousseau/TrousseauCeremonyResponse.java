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
public class TrousseauCeremonyResponse {

    private Long id;
    private Long boardId;
    private String ceremonyType;
    private String title;
    private String colorTheme;
    private BigDecimal targetBudget;
    private Integer displayOrder;

    @Builder.Default
    private int itemCount = 0;

    @Builder.Default
    private BigDecimal allocatedSpend = BigDecimal.ZERO;

    @Builder.Default
    private List<TrousseauItemResponse> items = new ArrayList<>();

    private LocalDateTime createdAt;
}
