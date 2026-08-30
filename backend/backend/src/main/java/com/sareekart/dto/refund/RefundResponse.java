package com.sareekart.dto.refund;

import com.sareekart.entity.Refund;
import com.sareekart.entity.enums.RefundReasonCode;
import com.sareekart.entity.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Admin-facing refund record (includes gateway reference and failure detail). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long paymentId;
    private String providerRefundId;
    private BigDecimal amount;
    private RefundStatus status;
    private String reason;
    private RefundReasonCode reasonCode;
    private String initiatedBy;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static RefundResponse from(Refund r) {
        return RefundResponse.builder()
            .id(r.getId())
            .orderId(r.getOrder() != null ? r.getOrder().getId() : null)
            .orderNumber(r.getOrder() != null ? r.getOrder().getOrderNumber() : null)
            .paymentId(r.getPayment() != null ? r.getPayment().getId() : null)
            .providerRefundId(r.getProviderRefundId())
            .amount(r.getAmount())
            .status(r.getStatus())
            .reason(r.getReason())
            .reasonCode(r.getReasonCode())
            .initiatedBy(r.getInitiatedBy())
            .errorMessage(r.getErrorMessage())
            .createdAt(r.getCreatedAt())
            .build();
    }
}
