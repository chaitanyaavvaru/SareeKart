package com.sareekart.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreditRequest {
    private BigDecimal amount;
    private Integer points;
    @NotNull(message = "Reason / description is required")
    private String reason;
}
