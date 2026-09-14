package com.sareekart.dto.response.customer;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAffinityResponse {
    private Long userId;
    private String preferredFabric;
    private String preferredWeave;
    private String preferredColor;
    private String priceSensitivity;
    private int purchaseIntentScore;
    private long totalViews;
    private long totalCartAdds;
    private long totalPurchases;
    private LocalDateTime lastActiveAt;
    private Map<String, Integer> fabricWeights;
    private Map<String, Integer> colorWeights;
    private Map<String, Object> scoreBreakdown;
}
