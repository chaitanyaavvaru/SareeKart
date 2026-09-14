package com.sareekart.dto.response.customer;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehavioralOverviewResponse {
    private String range;
    private long totalEvents;
    private long activeSessions;
    private Map<String, Long> eventCountsByType;
    private List<BehavioralFunnelStageDto> funnel;
    private List<TopTrendingProductDto> topProducts;
    private List<SearchQueryTelemetryDto> topSearches;
    private List<SearchQueryTelemetryDto> zeroResultSearches;
    private List<CustomerEventResponse> recentEvents;
}
