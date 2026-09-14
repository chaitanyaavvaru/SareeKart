package com.sareekart.dto.response.customer;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehavioralFunnelStageDto {
    private String stage;
    private String label;
    private long totalEvents;
    private long uniqueSessions;
    private double conversionRateFromPrevious;
    private double overallConversionRate;
}
