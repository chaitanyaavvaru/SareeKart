package com.sareekart.dto.trousseau;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedTrousseauViewResponse {

    private String shareToken;
    private String title;
    private String brideOrOwnerName;
    private LocalDate weddingDate;
    private String notes;
    private Boolean isPublicVoting;
    private String status;

    @Builder.Default
    private int totalCeremonies = 0;

    @Builder.Default
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Builder.Default
    private List<TrousseauCeremonyResponse> ceremonies = new ArrayList<>();
}
