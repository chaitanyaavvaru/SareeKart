package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrapeStyleResponse {

    private Long consultationId;
    private Long sareeId;
    private String sareeName;
    private String fabric;
    private String primaryColor;
    private String occasion;
    private List<EnsembleLook> curatedLooks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnsembleLook {
        private String id;
        private String title;
        private String subtitle;
        private String description;
        private BlouseRecommendation blouse;
        private JewelryRecommendation jewelry;
        private AccentRecommendation accents;
        private String drapingTechnique;
        private String stylingRationale;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlouseRecommendation {
        private String fabric;
        private String contrastColor;
        private String colorHex;
        private String frontNeck;
        private String backNeck;
        private String sleeve;
        private String blouseStyle; // "tailored" or "designer"
        private String recommendedWork;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JewelryRecommendation {
        private String category;
        private String necklace;
        private String earrings;
        private String bangles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccentRecommendation {
        private String hairFlorals;
        private String footwear;
        private String potliBag;
    }
}
