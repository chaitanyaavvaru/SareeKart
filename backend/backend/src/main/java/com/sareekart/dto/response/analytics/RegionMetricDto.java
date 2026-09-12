package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionMetricDto {
    private String region;
    private String name;
    private String state;
    private Integer orderCount;
    private BigDecimal revenue;
    private Double percentage;
}
