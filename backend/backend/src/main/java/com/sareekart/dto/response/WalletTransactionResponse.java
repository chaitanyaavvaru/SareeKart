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
public class WalletTransactionResponse {
    private Long id;
    private Long walletId;
    private BigDecimal amount;
    private Integer points;
    private String type;
    private String description;
    private Long referenceId;
    private String referenceType;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}
