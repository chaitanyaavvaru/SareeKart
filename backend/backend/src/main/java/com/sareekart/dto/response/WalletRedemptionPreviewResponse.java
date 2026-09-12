package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRedemptionPreviewResponse {
    private BigDecimal walletBalance;
    private BigDecimal maxRedeemable;
    private BigDecimal remainingOrderTotal;
    private boolean fullyCovered;
}
