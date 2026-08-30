package com.sareekart.dto.refund;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sareekart.entity.enums.RefundReasonCode;
import java.math.BigDecimal;

/**
 * Admin refund initiation payload. Absent/null amount ⇒ FULL refund of the
 * remaining refundable balance (derived server-side). Any amount supplied is
 * validated against persisted payment data, never trusted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @DecimalMin(value = "0.01")
    private BigDecimal amount; // null/absent ⇒ full refund

    @jakarta.validation.constraints.Size(max = 255)
    private String reason;

    /** Optional structured code; defaults to OTHER when absent. */
    private RefundReasonCode reasonCode;
}