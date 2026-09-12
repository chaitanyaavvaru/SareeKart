package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDistributionDto {
    private String method;
    private Long count;
    private BigDecimal amount;
    private Double percentage;
}
