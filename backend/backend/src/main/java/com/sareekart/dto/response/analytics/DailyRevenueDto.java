package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRevenueDto {
    private String date;
    private BigDecimal revenue;
    private Long orders;
    private Long units;
}
