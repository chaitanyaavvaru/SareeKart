package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userName;
    private BigDecimal balance;
    private Integer loyaltyPoints;
    private String tier;
    private String tierDisplayName;
    private Double pointsMultiplier;
    private BigDecimal lifetimeSpent;
    private BigDecimal nextTierSpendRemaining;
    private Integer totalTransactions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
