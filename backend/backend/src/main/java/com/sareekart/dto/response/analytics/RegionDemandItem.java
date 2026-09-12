package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionDemandItem {
    private String name;
    private String region;
    private String state;
    private Long orderCount;
    private BigDecimal revenue;
    private Double percentage;

    public String getName() {
        return name != null ? name : region;
    }

    public String getRegion() {
        return region != null ? region : name;
    }
}
