package com.sareekart.dto.response.analytics;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionalDemandSummary {
    private List<RegionDemandItem> topStates;
    private List<RegionDemandItem> topCities;
}
