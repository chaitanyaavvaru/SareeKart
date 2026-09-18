package com.sareekart.dto.trousseau;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrousseauBoardResponse {

    private Long id;
    private Long userId;
    private String ownerName;
    private String title;
    private LocalDate weddingDate;
    private String notes;
    private String shareToken;
    private String shareUrl;
    private Boolean isPublicVoting;
    private String status;

    @Builder.Default
    private int totalCeremonies = 0;

    @Builder.Default
    private BigDecimal totalBudget = BigDecimal.ZERO;

    @Builder.Default
    private int totalItemsCount = 0;

    @Builder.Default
    private BigDecimal totalAllocatedSpend = BigDecimal.ZERO;

    @Builder.Default
    private List<TrousseauCeremonyResponse> ceremonies = new ArrayList<>();

    @Builder.Default
    private List<TrousseauCollaboratorResponse> collaborators = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
