package com.sareekart.dto.response.analytics;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAnalyticsResponse {

    // Customer LTV tiers
    private List<LtvTierSummary> ltvTiers;

    // New vs Returning customer cohort
    private Object newVsReturning;

    // Regional demand breakdown
    private Object regionalBreakdown;

    // Top regions directly accessible
    private List<RegionDemandItem> topStates;
    private List<RegionDemandItem> topCities;

    // Cart abandonment & conversion funnel
    private Object conversionFunnel;

    // Range context
    private String range;
    private Object startDate;
    private Object endDate;
}
