package com.sareekart.dto.trousseau;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrousseauAiCurationResponse {

    private Long boardId;
    private Long ceremonyId;
    private String ceremonyType;
    private String colorTheme;
    private BigDecimal ceremonyBudget;
    private String overallStylingNote;
    private List<EnsembleRecommendation> recommendations;
    private boolean fallbackUsed;
    private int totalCandidatesEvaluated;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnsembleRecommendation {
        private Long productId;
        private String productName;
        private BigDecimal productPrice;
        private String productImageUrl;
        private String fabric;
        private String color;
        private Integer stockQuantity;
        private Double matchConfidence;
        private String stylingReasoning;
        private String contrastBlouse;
        private String jewelryPairing;
        private String drapeStyle;
    }
}
