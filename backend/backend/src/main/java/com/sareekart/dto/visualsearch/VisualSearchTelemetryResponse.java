package com.sareekart.dto.visualsearch;

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
public class VisualSearchTelemetryResponse {
    private long totalSearches;
    private long searchesToday;
    private double avgConfidenceScore;
    private int avgLatencyMs;
    private List<VisualSearchQueryLogDto> recentLogs;
    private List<Map<String, Object>> topColors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisualSearchQueryLogDto {
        private String id;
        private String source;
        private String attributes;
        private String matchSku;
        private String confidence;
        private String status;
        private String createdAt;
    }
}
