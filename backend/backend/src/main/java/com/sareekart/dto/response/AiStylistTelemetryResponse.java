package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStylistTelemetryResponse {

    private long totalConsultations;
    private long convertedToTailoringCount;
    private double tailoringConversionRatePercent;
    private List<Map<String, Object>> topOccasions;
    private List<Map<String, Object>> topStyledSarees;
    private List<ConsultationSummary> recentConsultations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsultationSummary {
        private Long id;
        private String sareeName;
        private String occasion;
        private String chosenLookTitle;
        private String contrastColor;
        private String blouseStyle;
        private boolean convertedToTailoring;
        private String createdAt;
    }
}
